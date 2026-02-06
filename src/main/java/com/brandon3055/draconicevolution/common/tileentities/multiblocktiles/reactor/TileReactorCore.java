package com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.reactor;

import static com.brandon3055.draconicevolution.common.container.ContainerReactor.blockFuelAmount;
import static com.brandon3055.draconicevolution.common.container.ContainerReactor.fullChaosAmount;
import static com.brandon3055.draconicevolution.common.container.ContainerReactor.ingotFuelAmount;
import static com.brandon3055.draconicevolution.common.container.ContainerReactor.maximumFuelStorage;
import static com.brandon3055.draconicevolution.common.container.ContainerReactor.nuggetFuelAmount;
import static com.brandon3055.draconicevolution.common.handler.ConfigHandler.linearReactorFuelUsage;
import static com.brandon3055.draconicevolution.common.handler.ConfigHandler.reactorOutputMultiplier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.client.ReactorSound;
import com.brandon3055.draconicevolution.client.gui.GuiHandler;
import com.brandon3055.draconicevolution.client.render.particle.Particles;
import com.brandon3055.draconicevolution.common.ModItems;
import com.brandon3055.draconicevolution.common.blocks.multiblock.IReactorPart;
import com.brandon3055.draconicevolution.common.blocks.multiblock.IReactorPart.ComparatorMode;
import com.brandon3055.draconicevolution.common.blocks.multiblock.MultiblockHelper.TileLocation;
import com.brandon3055.draconicevolution.common.handler.ConfigHandler;
import com.brandon3055.draconicevolution.common.lib.References;
import com.brandon3055.draconicevolution.common.tileentities.TileObjectSync;
import com.brandon3055.draconicevolution.common.utils.OreDictionaryHelper;
import com.brandon3055.draconicevolution.common.utils.Utils;
import com.brandon3055.draconicevolution.common.utils.handlers.ProcessHandler;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Created by Brandon on 16/6/2015.
 */
public class TileReactorCore extends TileObjectSync implements IInventory {

    public static final int MAXIMUM_PART_DISTANCE = 10;

    public enum ReactorState {

        OFFLINE,
        STARTING,
        ONLINE,
        STOPPING,
        INVALID;

        private static final ReactorState[] values = values();

        public static ReactorState getState(int ordinal) {
            return ordinal >= 0 && ordinal < values.length ? values[ordinal] : INVALID;
        }

        public String toLocalizedString(boolean canStart) {
            return StatCollector.translateToLocal(
                    this == STARTING && canStart ? "gui.de.status1_5.txt" : "gui.de.status" + ordinal() + ".txt");
        }
    }

    public ReactorState reactorState = ReactorState.OFFLINE;
    public float renderRotation = 0;
    public float renderSpeed = 0;
    public boolean isStructureValid = false;
    public float stabilizerRender = 0F;
    private boolean startupInitialized = false;
    public int tick = 0;

    // Key operational figures
    public int reactorFuel = 0;
    public int convertedFuel = 0; // The amount of fuel that has converted
    public double conversionUnit = 0; // used to smooth out the conversion between int and floating point. When >= 1
                                      // minus one and convert one int worth of fuel

    public double reactionTemperature = 20;
    public double maxReactTemperature = 10000;

    public double fieldCharge = 0;
    public double maxFieldCharge = 0;

    public int energySaturation = 0;
    public int maxEnergySaturation = 0;

    public float fuelStorageRatio = (float) ConfigHandler.reactorFuelStorage / 10368; // To keep the temperature
                                                                                      // increase unchanged from the
                                                                                      // default config if it's changed

    int excessFuel = 0;

    @SideOnly(Side.CLIENT)
    private ReactorSound reactorSound;

    public List<TileLocation> stabilizerLocations = new ArrayList<>();

    @Override
    public void updateEntity() {
        tick++;
        if (worldObj.isRemote) {
            updateSound();

            renderSpeed = (float) Math.min((reactionTemperature - 20) / 2000D, 1D);
            stabilizerRender = (float) Math.min(fieldCharge / (maxFieldCharge * 0.1D), 1D);
            renderRotation += renderSpeed;
            checkPlayerCollision();
            return;
        }

        switch (reactorState) {
            case OFFLINE -> offlineTick();
            case STARTING -> startingTick();
            case ONLINE, STOPPING -> runTick();
        }

        if (excessFuel > 0 || (reactorFuel + convertedFuel) < maximumFuelStorage) {
            int useExcessFuel = Math.min(maximumFuelStorage - (reactorFuel + convertedFuel), excessFuel);
            reactorFuel += useExcessFuel;
            excessFuel -= useExcessFuel;
        }
        detectAndSendChanges();
    }

    @SideOnly(Side.CLIENT)
    private void updateSound() {
        if (reactorSound == null) {
            reactorSound = (ReactorSound) DraconicEvolution.proxy.playISound(new ReactorSound(this));
        }
    }

    private void checkPlayerCollision() {
        EntityPlayer player = DraconicEvolution.proxy.getClientPlayer();
        double distance = Utils
                .getDistanceAtoB(player.posX, player.posY, player.posZ, xCoord + 0.5, yCoord + 0.5, zCoord + 0.5);
        double coreRadiusWithMargin = getCoreRadius() + 0.5;
        if (distance < coreRadiusWithMargin) {
            double distanceMod = 1D - (distance / Math.max(0.1, coreRadiusWithMargin));
            double offsetX = player.posX - xCoord + 0.5;
            double offsetY = player.posY - yCoord + 0.5;
            double offsetZ = player.posZ - zCoord + 0.5;
            player.addVelocity(offsetX * distanceMod, offsetY * distanceMod, offsetZ * distanceMod);
        }
    }

    private void offlineTick() {
        if (reactionTemperature > 20) {
            reactionTemperature -= 0.5;
        }
        fieldCharge = Math.max(0, fieldCharge - maxFieldCharge * 0.0005);
        energySaturation = (int) Math.max(0, energySaturation - maxEnergySaturation * 0.000001);
    }

    private void startingTick() {
        if (!startupInitialized) {
            int totalFuel = reactorFuel + convertedFuel;
            maxFieldCharge = (totalFuel / fuelStorageRatio) * 96.45061728395062 * 100;
            maxEnergySaturation = (int) ((totalFuel / fuelStorageRatio) * 96.45061728395062 * 1000);
            energySaturation = Math.min(energySaturation, maxEnergySaturation);
            fieldCharge = Math.min(fieldCharge, maxFieldCharge);
            startupInitialized = true;
        }
    }

    private boolean hasExploded = false;

    public double tempDrainFactor;
    public double generationRate;
    public int fieldDrain;
    public double fieldInputRate;
    public double fuelUseRate;

    private void runTick() {
        // Inverted core saturation (if multiplied by 100 this creates infinite numbers which breaks the code)
        double saturation = (double) energySaturation / (double) maxEnergySaturation;
        double saturationI = (1D - saturation) * 99D;
        double temp = (reactionTemperature / maxReactTemperature) * 50D;
        // The conversion level. Ranges from -0.3 to 1.0
        double conversion = (convertedFuel + conversionUnit) / (convertedFuel + reactorFuel - conversionUnit) * 1.3
                - 0.3D;

        // Temperature Calculation
        final double tempOffset = 444.7; // Adjusts where the temp falls to at 100% saturation
        // The exponential temperature rise which increases as the core saturation goes down
        double tempRiseExpo = Math.pow(saturationI, 3) / (100 - saturationI) + tempOffset;
        // This is used to add resistance as the temp rises because the hotter something gets the more energy it takes
        // to get it hotter
        double tempRiseResist = Math.pow(temp, 4) / (100 - temp);
        // This puts all the numbers together and gets the value to raise or lower the temp by this tick. This is
        // dealing with very big numbers so the result is divided by 10000
        double riseAmount = (tempRiseExpo - tempRiseResist * (1D - conversion) + conversion * 1000) / 10000;
        if (reactorState == ReactorState.STOPPING) {
            if (reactionTemperature <= 2001) {
                reactorState = ReactorState.OFFLINE;
                startupInitialized = false;
                return;
            }
            if (energySaturation >= maxEnergySaturation * 0.99 && reactorFuel > 0) {
                reactionTemperature -= 1D - conversion;
            } else {
                reactionTemperature += riseAmount * 10;
            }
        } else {
            reactionTemperature += riseAmount * 10;
        }

        // Energy Calculation
        int baseMaxRFt = (int) ((maxEnergySaturation / 1000D) * reactorOutputMultiplier * 1.5D);
        int maxRFt = (int) (baseMaxRFt * (1D + conversion * 2));
        generationRate = (1D - saturation) * maxRFt;
        energySaturation += generationRate;

        // When temp < 1000 power drain is 0, when temp > 2000 power drain is 1, when temp > 8000 power drain increases
        // exponentially
        tempDrainFactor = reactionTemperature > 8000 ? 1 + (Math.pow(reactionTemperature - 8000, 2) * 0.0000025)
                : reactionTemperature > 2000 ? 1 : reactionTemperature > 1000 ? (reactionTemperature - 1000) / 1000 : 0;

        // Field Drain Calculation
        // (baseMaxRFt/make smaller to increase field power drain)
        fieldDrain = (int) Math.min(tempDrainFactor * (1D - saturation) * (baseMaxRFt / 10.923556), Integer.MAX_VALUE);

        double fieldNegPercent = 1D - fieldCharge / maxFieldCharge;
        fieldInputRate = fieldDrain / fieldNegPercent;
        fieldCharge -= fieldDrain;

        // Calculate Fuel Usage
        if (linearReactorFuelUsage) {
            fuelUseRate = tempDrainFactor * (1D - saturation) * (0.001 * ConfigHandler.reactorFuelUsageMultiplier);
        } else {
            fuelUseRate = tempDrainFactor * (1D - Math.pow(Math.max(saturation - 0.1, 0), 0.3))
                    * (0.001 * ConfigHandler.reactorFuelUsageMultiplier);
        }
        conversionUnit += fuelUseRate;
        if (conversionUnit >= 1 && reactorFuel > 0) {
            reactorFuel -= (int) conversionUnit;
            convertedFuel += (int) conversionUnit;
            conversionUnit -= (int) conversionUnit;
        }

        if (fieldCharge <= 0 && !hasExploded) {
            hasExploded = true;
            goBoom();
        }
    }

    private void goBoom() {
        if (ConfigHandler.enableReactorBigBoom) {
            float power = 2F + ((float) (convertedFuel + reactorFuel) / (maximumFuelStorage + 1F) * 18F);
            ProcessHandler.addProcess(new ReactorExplosion(worldObj, xCoord, yCoord, zCoord, power));
            sendObjectToClient(
                    References.INT_ID,
                    100,
                    (int) (power * 10F),
                    new NetworkRegistry.TargetPoint(worldObj.provider.dimensionId, xCoord, yCoord, zCoord, 512));
        } else {
            worldObj.createExplosion(null, xCoord, yCoord, zCoord, 5, true);
        }
        worldObj.setBlockToAir(xCoord, yCoord, zCoord);
    }

    private boolean isFacingToCore(int x, int y, int z, IReactorPart part) {
        ForgeDirection facing = part.getFacing();
        int offsetX = Integer.signum(xCoord - x);
        int offsetY = Integer.signum(yCoord - y);
        int offsetZ = Integer.signum(zCoord - z);
        return offsetX == facing.offsetX && offsetY == facing.offsetY && offsetZ == facing.offsetZ;
    }

    public void updateReactorParts(boolean shouldSetUp) {
        stabilizerLocations.clear();
        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            for (int distance = 1; distance <= MAXIMUM_PART_DISTANCE; distance++) {
                int targetX = xCoord + direction.offsetX * distance;
                int targetY = yCoord + direction.offsetY * distance;
                int targetZ = zCoord + direction.offsetZ * distance;
                TileEntity tile = worldObj.getTileEntity(targetX, targetY, targetZ);
                if (tile instanceof IReactorPart part && isFacingToCore(targetX, targetY, targetZ, part)) {
                    if (shouldSetUp) {
                        part.setUp(new TileLocation(xCoord, yCoord, zCoord));
                    }
                    if (tile instanceof TileReactorStabilizer) {
                        stabilizerLocations.add(new TileLocation(targetX, targetY, targetZ));
                    }
                    break;
                }
            }
        }
    }

    public void validateStructure() {
        final double margin = 0.5D;
        final double coreRadiusSquared = Math.pow(getCoreRadius() + margin, 2);

        boolean updateRequired = false;
        int stabilizersCount = 0;
        Iterator<TileLocation> iterator = stabilizerLocations.iterator();
        while (iterator.hasNext()) {
            TileLocation location = iterator.next();
            if (location.getDistanceSquared(xCoord, yCoord, zCoord) > coreRadiusSquared) {
                TileEntity tile = location.getTileEntity(worldObj);
                if (tile instanceof TileReactorStabilizer stabilizer) {
                    if (stabilizer.masterLocation.isThisLocation(xCoord, yCoord, zCoord)) {
                        stabilizersCount++;
                        continue;
                    }
                }
            }
            iterator.remove();
            updateRequired = true;
        }

        isStructureValid = stabilizersCount == 4;
        if (isStructureValid) {
            if (reactorState == ReactorState.INVALID) {
                reactorState = ReactorState.OFFLINE;
            }
        } else {
            reactorState = ReactorState.INVALID;
            if (reactionTemperature >= 2000) {
                goBoom();
            }
        }

        if (updateRequired) {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    public void onPlaced() {
        updateReactorParts(true);
        validateStructure();
    }

    public void onBroken() {
        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            for (int distance = 1; distance <= MAXIMUM_PART_DISTANCE; distance++) {
                TileEntity tile = worldObj.getTileEntity(
                        xCoord + direction.offsetX * distance,
                        yCoord + direction.offsetY * distance,
                        zCoord + direction.offsetZ * distance);
                if (tile instanceof IReactorPart part) {
                    if (part.getMasterLocation().isThisLocation(xCoord, yCoord, zCoord)) {
                        part.shutDown();
                    }
                    break;
                }
            }
        }
        if (reactionTemperature >= 2000) {
            goBoom();
        }
    }

    public boolean onStructureRightClicked(EntityPlayer player) {
        if (!worldObj.isRemote) {
            player.openGui(DraconicEvolution.instance, GuiHandler.GUIID_REACTOR, worldObj, xCoord, yCoord, zCoord);
        }
        return true;
    }

    public int injectEnergy(int energyToInject) {
        int energyInjected = 0;
        if (reactorState == ReactorState.STARTING) {
            if (!startupInitialized) {
                return 0;
            }
            if (fieldCharge < maxFieldCharge / 2) {
                energyInjected = Math.min(energyToInject, (int) (maxFieldCharge / 2) - (int) fieldCharge + 1);
                fieldCharge += energyInjected;
                if (fieldCharge > maxFieldCharge / 2) {
                    fieldCharge = maxFieldCharge / 2;
                }
            } else if (energySaturation < maxEnergySaturation / 2) {
                energyInjected = Math.min(energyToInject, maxEnergySaturation / 2 - energySaturation);
                energySaturation += energyInjected;
            } else if (reactionTemperature < 2000) {
                energyInjected = energyToInject;
                reactionTemperature += (double) energyInjected / (1000D + (reactorFuel / fuelStorageRatio) * 10);
                if (reactionTemperature > 2000) {
                    reactionTemperature = 2000;
                }
            }
        } else if (reactorState == ReactorState.ONLINE || reactorState == ReactorState.STOPPING) {
            energyInjected = energyToInject;
            fieldCharge += energyInjected * (1D - fieldCharge / maxFieldCharge);
            fieldCharge = Math.min(fieldCharge, maxFieldCharge);
        }
        return energyInjected;
    }

    public boolean canStart() {
        return isStructureValid && reactionTemperature >= 2000
                && fieldCharge >= maxFieldCharge / 2
                && energySaturation >= maxEnergySaturation / 2
                && convertedFuel + reactorFuel + conversionUnit >= 144;
    }

    public boolean canCharge() {
        return isStructureValid && reactorState != ReactorState.ONLINE
                && convertedFuel + reactorFuel + conversionUnit >= 144;
    }

    public boolean canStop() {
        return isStructureValid && reactorState != ReactorState.OFFLINE;
    }

    public void processButtonPress(int button) {
        if (button == 0 && canCharge()) {
            reactorState = ReactorState.STARTING;
        } else if (button == 1 && canStart()) {
            reactorState = ReactorState.ONLINE;
        } else if (button == 2 && canStop()) {
            reactorState = ReactorState.STOPPING;
        }
    }

    public double getCoreRadius() {
        double volume = (double) (reactorFuel + convertedFuel) / (1296D * fuelStorageRatio);
        volume *= 1 + (reactionTemperature / maxReactTemperature) * 10D;
        return Math.cbrt(volume / (4D / 3D * Math.PI));
    }

    public double getCoreDiameter() {
        return getCoreRadius() * 2;
    }

    private boolean isStructureValidCache = false;
    private boolean startupInitializedCache = false;
    private int reactorStateCache = -1;
    private int reactorFuelCache = -1;
    private int convertedFuelCache = -1;
    private int energySaturationCache = -1;
    private int maxEnergySaturationCache = -1;
    private double reactionTemperatureCache = -1;
    private double maxReactTemperatureCache = -1;
    private double fieldChargeCache = -1;
    private double maxFieldChargeCache = -1;

    private void detectAndSendChanges() {
        NetworkRegistry.TargetPoint targetPoint = new NetworkRegistry.TargetPoint(
                worldObj.provider.dimensionId,
                xCoord,
                yCoord,
                zCoord,
                128);
        if (reactionTemperatureCache != reactionTemperature) {
            reactionTemperatureCache = (double) sendObjectToClient(
                    References.DOUBLE_ID,
                    8,
                    reactionTemperature,
                    targetPoint);
        }
        if (tick % 10 != 0) {
            return;
        }
        if (isStructureValidCache != isStructureValid) {
            isStructureValidCache = (boolean) sendObjectToClient(
                    References.BOOLEAN_ID,
                    0,
                    isStructureValid,
                    targetPoint);
        }
        if (startupInitializedCache != startupInitialized) {
            startupInitializedCache = (boolean) sendObjectToClient(
                    References.BOOLEAN_ID,
                    2,
                    startupInitialized,
                    targetPoint);
        }
        if (reactorStateCache != reactorState.ordinal()) {
            reactorStateCache = (int) sendObjectToClient(References.INT_ID, 3, reactorState.ordinal(), targetPoint);
        }
        if (reactorFuelCache != reactorFuel) {
            reactorFuelCache = (int) sendObjectToClient(References.INT_ID, 4, reactorFuel, targetPoint);
        }
        if (convertedFuelCache != convertedFuel) {
            convertedFuelCache = (int) sendObjectToClient(References.INT_ID, 5, convertedFuel, targetPoint);
        }
        if (energySaturationCache != energySaturation) {
            energySaturationCache = (int) sendObjectToClient(References.INT_ID, 6, energySaturation, targetPoint);
        }
        if (maxEnergySaturationCache != maxEnergySaturation) {
            maxEnergySaturationCache = (int) sendObjectToClient(References.INT_ID, 7, maxEnergySaturation, targetPoint);
        }
        if (maxReactTemperatureCache != maxReactTemperature) {
            maxReactTemperatureCache = (double) sendObjectToClient(
                    References.DOUBLE_ID,
                    9,
                    maxReactTemperature,
                    targetPoint);
        }
        if (fieldChargeCache != fieldCharge) {
            fieldChargeCache = (double) sendObjectToClient(References.DOUBLE_ID, 10, fieldCharge, targetPoint);
        }
        if (maxFieldChargeCache != maxFieldCharge) {
            maxFieldChargeCache = (double) sendObjectToClient(References.DOUBLE_ID, 11, maxFieldCharge, targetPoint);
        }
    }

    public int getComparatorOutput(ComparatorMode comparatorMode) {
        return switch (comparatorMode) {
            case TEMPERATURE -> toRedstoneStrength(reactionTemperature, maxReactTemperature, comparatorMode);
            case TEMPERATURE_INVERTED -> 15
                    - toRedstoneStrength(reactionTemperature, maxReactTemperature, comparatorMode);
            case FIELD_CHARGE -> toRedstoneStrength(fieldCharge, maxFieldCharge, comparatorMode);
            case FIELD_CHARGE_INVERTED -> 15 - toRedstoneStrength(fieldCharge, maxFieldCharge, comparatorMode);
            case ENERGY_SATURATION -> toRedstoneStrength(energySaturation, maxEnergySaturation, comparatorMode);
            case ENERGY_SATURATION_INVERTED -> 15
                    - toRedstoneStrength(energySaturation, maxEnergySaturation, comparatorMode);
            case CONVERTED_FUEL -> toRedstoneStrength(
                    convertedFuel + conversionUnit,
                    reactorFuel - conversionUnit,
                    comparatorMode);
            case CONVERTED_FUEL_INVERTED -> 15
                    - toRedstoneStrength(convertedFuel + conversionUnit, reactorFuel - conversionUnit, comparatorMode);
        };
    }

    private int toRedstoneStrength(double value, double maxValue, ComparatorMode comparatorMode) {
        if (maxValue == 0) {
            return 0;
        }
        double proportion = value / maxValue;
        int redstoneStrength = (int) (proportion * 15D);
        switch (comparatorMode) {
            case FIELD_CHARGE, FIELD_CHARGE_INVERTED -> {
                if (proportion < 0.1) {
                    redstoneStrength = 0;
                }
            }
            case CONVERTED_FUEL, CONVERTED_FUEL_INVERTED -> {
                if (proportion > 0.9) {
                    redstoneStrength = 15;
                }
            }
        }
        return redstoneStrength;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void receiveObjectFromServer(int index, Object object) {
        switch (index) {
            case 0 -> isStructureValid = (boolean) object;
            case 2 -> startupInitialized = (boolean) object;
            case 3 -> reactorState = ReactorState.getState((int) object);
            case 4 -> reactorFuel = (int) object;
            case 5 -> convertedFuel = (int) object;
            case 6 -> energySaturation = (int) object;
            case 7 -> maxEnergySaturation = (int) object;
            case 8 -> reactionTemperature = (double) object;
            case 9 -> maxReactTemperature = (double) object;
            case 10 -> fieldCharge = (double) object;
            case 11 -> maxFieldCharge = (double) object;
            case 100 -> FMLClientHandler.instance().getClient().effectRenderer
                    .addEffect(new Particles.ReactorExplosionParticle(worldObj, xCoord, yCoord, zCoord, (int) object));
        }
        super.receiveObjectFromServer(index, object);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }

    @Override
    public double getMaxRenderDistanceSquared() {
        return 40960.0D;
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound compound = new NBTTagCompound();
        writeToNBT(compound);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, compound);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        readFromNBT(pkt.func_148857_g());
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);

        NBTTagList stabilizerList = new NBTTagList();
        for (TileLocation offset : stabilizerLocations) {
            NBTTagCompound compound1 = new NBTTagCompound();
            offset.writeToNBT(compound1, "tag");
            stabilizerList.appendTag(compound1);
        }
        if (stabilizerList.tagCount() > 0) compound.setTag("Stabilizers", stabilizerList);

        compound.setByte("State", (byte) reactorState.ordinal());
        compound.setBoolean("isStructureValid", isStructureValid);
        compound.setBoolean("startupInitialized", startupInitialized);
        compound.setInteger("energySaturation", energySaturation);
        compound.setInteger("maxEnergySaturation", maxEnergySaturation);
        compound.setInteger("reactorFuel", reactorFuel);
        compound.setInteger("convertedFuel", convertedFuel);
        compound.setInteger("excessFuel", excessFuel);
        compound.setDouble("reactionTemperature", reactionTemperature);
        compound.setDouble("maxReactTemperature", maxReactTemperature);
        compound.setDouble("fieldCharge", fieldCharge);
        compound.setDouble("maxFieldCharge", maxFieldCharge);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        stabilizerLocations = new ArrayList<>();
        if (compound.hasKey("Stabilizers")) {
            NBTTagList stabilizerList = compound.getTagList("Stabilizers", 10);
            for (int i = 0; i < stabilizerList.tagCount(); i++) {
                TileLocation offset = new TileLocation();
                offset.readFromNBT(stabilizerList.getCompoundTagAt(i), "tag");
                stabilizerLocations.add(offset);
            }
        }

        reactorState = ReactorState.getState(compound.getByte("State"));
        isStructureValid = compound.getBoolean("isStructureValid");
        startupInitialized = compound.getBoolean("startupInitialized");
        energySaturation = compound.getInteger("energySaturation");
        maxEnergySaturation = compound.getInteger("maxEnergySaturation");
        reactorFuel = compound.getInteger("reactorFuel");
        convertedFuel = compound.getInteger("convertedFuel");
        excessFuel = compound.getInteger("excessFuel");
        reactionTemperature = compound.getDouble("reactionTemperature");
        maxReactTemperature = compound.getDouble("maxReactTemperature");
        fieldCharge = compound.getDouble("fieldCharge");
        maxFieldCharge = compound.getDouble("maxFieldCharge");
    }

    // todo review wtf
    public Object[] callMethod(String methodName, Object... args) {
        if (args.length > 0) {
            throw new IllegalArgumentException("This method does not accept arguments");
        }

        if (methodName.equals("getReactorInfo")) {
            Map<Object, Object> map = new HashMap<>();
            map.put("temperature", Utils.round(reactionTemperature, 100));
            map.put("fieldStrength", Utils.round(fieldCharge, 100));
            map.put("maxFieldStrength", Utils.round(maxFieldCharge, 100));
            map.put("energySaturation", energySaturation);
            map.put("maxEnergySaturation", maxEnergySaturation);
            map.put("fuelConversion", Utils.round(convertedFuel + conversionUnit, 1000));
            map.put("maxFuelConversion", reactorFuel + convertedFuel);
            map.put("generationRate", (int) generationRate);
            map.put("fieldDrainRate", fieldDrain);
            map.put("fuelConversionRate", (int) Math.round(fuelUseRate * 1000000D));
            if (reactorState == ReactorState.STARTING) {
                map.put("status", canStart() ? "charged" : "charging");
            } else {
                map.put("status", reactorState.name().toLowerCase());
            }
            return new Object[] { map };
        }
        if (methodName.equals("chargeReactor")) {
            if (canCharge()) {
                reactorState = ReactorState.STARTING;
                return new Object[] { true };
            }
            return new Object[] { false };
        }
        if (methodName.equals("activateReactor")) {
            if (canStart()) {
                reactorState = ReactorState.ONLINE;
                return new Object[] { true };
            }
            return new Object[] { false };
        }
        if (methodName.equals("stopReactor")) {
            if (canStop()) {
                reactorState = ReactorState.STOPPING;
                return new Object[] { true };
            }
            return new Object[] { false };
        }
        return new Object[] {};
    }

    @Override
    public int getSizeInventory() {
        return 1;

    }

    public ItemStack getStackInSlot(int i) {
        if (convertedFuel > fullChaosAmount) {
            return new ItemStack(ModItems.chaosShard, convertedFuel / fullChaosAmount);
        }
        return null;
    }

    @Override
    public ItemStack decrStackSize(int i, int count) {
        if (convertedFuel > fullChaosAmount) {
            int chaosShardCount = Math.min((convertedFuel / fullChaosAmount), count);
            convertedFuel -= chaosShardCount * fullChaosAmount;
            return new ItemStack(ModItems.chaosShard, chaosShardCount);
        }
        return null;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int i) {
        return null;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        if (fuelValue(stack) > 0) {
            int fuelValue = fuelValue(stack) * stack.stackSize;
            if (fuelValue > maximumFuelStorage - (reactorFuel + convertedFuel)) {
                fuelValue -= maximumFuelStorage - (reactorFuel + convertedFuel);
                reactorFuel = maximumFuelStorage - convertedFuel;
                excessFuel += fuelValue;
            } else {
                reactorFuel += fuelValue;
            }
        } else if (stack != null) {
            convertedFuel = stack.stackSize * fullChaosAmount;
        } else {
            convertedFuel -= (convertedFuel / fullChaosAmount) * fullChaosAmount;
        }
    }

    @Override
    public String getInventoryName() {
        return "";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        if (worldObj == null) {
            return true;
        }
        if (worldObj.getTileEntity(xCoord, yCoord, zCoord) != this) {
            return false;
        }
        return player.getDistanceSq(xCoord + 0.5, yCoord + 0.5, zCoord + 0.4) < 64;
    }

    @Override
    public void openInventory() {
        System.out.println("open");
    }

    @Override
    public void closeInventory() {
        System.out.println("close");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack item) {
        return false;
    }

    public int fuelValue(ItemStack item) {
        Set<String> oreNames = OreDictionaryHelper.getOreNames(item);

        if (oreNames.contains("blockDraconiumAwakened")) {
            return blockFuelAmount;
        }
        if (oreNames.contains("ingotDraconiumAwakened")) {
            return ingotFuelAmount;
        }
        if (oreNames.contains("nuggetDraconiumAwakened")) {
            return nuggetFuelAmount;
        }
        return 0;
    }
}
