package com.brandon3055.draconicevolution.common.tileentities.multiblocktiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.util.ForgeDirection;

import com.brandon3055.draconicevolution.api.IExtendedRFStorage;
import com.brandon3055.draconicevolution.client.handler.ParticleHandler;
import com.brandon3055.draconicevolution.client.render.particle.Particles;
import com.brandon3055.draconicevolution.common.ModBlocks;
import com.brandon3055.draconicevolution.common.blocks.multiblock.MultiblockHelper.TileLocation;
import com.brandon3055.draconicevolution.common.lib.References;
import com.brandon3055.draconicevolution.common.tileentities.TileObjectSync;
import com.brandon3055.draconicevolution.integration.computers.IDEPeripheral;

import cofh.api.energy.IEnergyHandler;
import cofh.api.energy.IEnergyReceiver;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.interfaces.tileentity.IBasicEnergyContainer;
import gregtech.api.interfaces.tileentity.IEnergyConnected;

/**
 * Created by Brandon on 28/07/2014.
 */
@Optional.Interface(iface = "gregtech.api.interfaces.tileentity.IEnergyConnected", modid = "gregtech")
public class TileEnergyPylon extends TileObjectSync
        implements IEnergyConnected, IEnergyHandler, IExtendedRFStorage, IDEPeripheral {

    public boolean active = false;
    public boolean lastTickActive = false;
    public boolean reciveEnergy = false; // Power Flow to system
    public boolean lastTickReciveEnergy = false;
    public float modelRotation = 0;
    public float modelScale = 0;
    private List<TileLocation> coreLocations = new ArrayList<>();
    private int selectedCore = 0;
    private byte particleRate = 0;
    private byte lastTickParticleRate = 0;
    private int lastCheckCompOverride = 0;
    private int tick = 0;

    @Override
    public void updateEntity() {
        if (worldObj.isRemote) {
            if (active) {
                modelRotation += 1.5;
                modelScale += reciveEnergy ? 0.01F : -0.01F;
                if (modelScale < 0) {
                    modelScale = reciveEnergy ? 0F : 10000F;
                }
                spawnParticles();
            } else {
                modelScale = 0.5F;
            }
            return;
        }

        tick++;
        if (tick % 20 == 0) {
            int cOut = (int) (getEnergyStored() / getMaxEnergyStored() * 15D);
            if (cOut != lastCheckCompOverride) {
                worldObj.notifyBlocksOfNeighborChange(
                        xCoord,
                        yCoord,
                        zCoord,
                        worldObj.getBlock(xCoord, yCoord, zCoord));
                lastCheckCompOverride = cOut;
            }
        }

        if (active && !reciveEnergy) {
            for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
                TileEntity tile = worldObj.getTileEntity(
                        xCoord + direction.offsetX,
                        yCoord + direction.offsetY,
                        zCoord + direction.offsetZ);
                if (tile instanceof IEnergyReceiver) {
                    int energyToExtract = extractEnergy(direction, Integer.MAX_VALUE, true);
                    int energyReceived = ((IEnergyReceiver) tile)
                            .receiveEnergy(direction.getOpposite(), energyToExtract, false);
                    extractEnergy(direction, energyReceived, false);
                } else if (tile instanceof IBasicEnergyContainer) {
                    IBasicEnergyContainer energyContainer = (IBasicEnergyContainer) tile;
                    long voltage = energyContainer.getInputVoltage();
                    long amperage = Math.min(
                            energyContainer.getInputAmperage(),
                            (energyContainer.getEUCapacity() - energyContainer.getStoredEU()) / voltage);
                    if (amperage > 0) {
                        long amperesDrained = drainEnergyUnits(voltage, amperage);
                        if (amperesDrained > 0) {
                            energyContainer.injectEnergyUnits(direction.getOpposite(), voltage, amperesDrained);
                        }
                    }
                }
            }
        }

        detectAndSendChanges();
        if (particleRate > 0) particleRate--;
    }

    public void onActivated() {
        if (!active) {
            active = isValidStructure();
        }
        findCores();
    }

    private TileEnergyStorageCore getMaster() {
        if (coreLocations.isEmpty()) return null;
        if (selectedCore >= coreLocations.size()) selectedCore = coreLocations.size() - 1;
        TileLocation core = coreLocations.get(selectedCore);
        if (core != null) {
            TileEntity tileEntity = worldObj.getTileEntity(core.getXCoord(), core.getYCoord(), core.getZCoord());
            if (tileEntity instanceof TileEnergyStorageCore) {
                return (TileEnergyStorageCore) tileEntity;
            }
        }
        return null;
    }

    private void findCores() {
        int yMod = worldObj.getBlockMetadata(xCoord, yCoord, zCoord) == 1 ? 15 : -15;
        int range = 15;
        List<TileLocation> locations = new ArrayList<>();
        for (int x = xCoord - range; x <= xCoord + range; x++) {
            for (int y = yCoord + yMod - range; y <= yCoord + yMod + range; y++) {
                for (int z = zCoord - range; z <= zCoord + range; z++) {
                    if (worldObj.getBlock(x, y, z) == ModBlocks.energyStorageCore) {
                        locations.add(new TileLocation(x, y, z));
                    }
                }
            }
        }

        if (locations != coreLocations) {
            coreLocations.clear();
            coreLocations.addAll(locations);
            selectedCore = selectedCore >= coreLocations.size() ? 0 : selectedCore;
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    public void nextCore() {
        findCores();
        selectedCore++;
        if (selectedCore >= coreLocations.size()) selectedCore = 0;
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    @SideOnly(Side.CLIENT)
    private void spawnParticles() {
        Random rand = worldObj.rand;
        TileEnergyStorageCore core = getMaster();
        if (core == null || !core.isOnline()) return;

        int x = core.xCoord;
        int y = core.yCoord;
        int z = core.zCoord;
        int cYCoord = worldObj.getBlockMetadata(xCoord, yCoord, zCoord) == 1 ? yCoord + 1 : yCoord - 1;

        float disMod;
        switch (core.getTier()) {
            case 0:
                disMod = 0.5F;
                break;
            case 1:
            case 2:
                disMod = 1F;
                break;
            case 3:
            case 4:
                disMod = 2F;
                break;
            case 5:
                disMod = 3F;
                break;
            default:
                disMod = 4F;
                break;
        }

        if (particleRate > 20) particleRate = 20;

        double sourceX = x + 0.5 - disMod + (rand.nextFloat() * (disMod * 2));
        double sourceY = y + 0.5 - disMod + (rand.nextFloat() * (disMod * 2));
        double sourceZ = z + 0.5 - disMod + (rand.nextFloat() * (disMod * 2));
        double targetX = xCoord + 0.5;
        double targetY = cYCoord + 0.5;
        double targetZ = zCoord + 0.5;
        if (rand.nextFloat() < 0.05F) {
            Particles.EnergyTransferParticle passiveParticle = reciveEnergy
                    ? new Particles.EnergyTransferParticle(
                            worldObj,
                            targetX,
                            targetY,
                            targetZ,
                            sourceX,
                            sourceY,
                            sourceZ,
                            true)
                    : new Particles.EnergyTransferParticle(
                            worldObj,
                            sourceX,
                            sourceY,
                            sourceZ,
                            targetX,
                            targetY,
                            targetZ,
                            true);
            ParticleHandler.spawnCustomParticle(passiveParticle, 35);
        }
        if (particleRate > 0) {
            if (particleRate > 10) {
                for (int i = 0; i <= particleRate / 10; i++) {
                    sourceX = x + 0.5 - disMod + (rand.nextFloat() * (disMod * 2));
                    sourceY = y + 0.5 - disMod + (rand.nextFloat() * (disMod * 2));
                    sourceZ = z + 0.5 - disMod + (rand.nextFloat() * (disMod * 2));
                    Particles.EnergyTransferParticle passiveParticle = reciveEnergy
                            ? new Particles.EnergyTransferParticle(
                                    worldObj,
                                    targetX,
                                    targetY,
                                    targetZ,
                                    sourceX,
                                    sourceY,
                                    sourceZ,
                                    false)
                            : new Particles.EnergyTransferParticle(
                                    worldObj,
                                    sourceX,
                                    sourceY,
                                    sourceZ,
                                    targetX,
                                    targetY,
                                    targetZ,
                                    false);
                    ParticleHandler.spawnCustomParticle(passiveParticle, 35);
                }
            } else if (rand.nextInt(Math.max(1, 10 - particleRate)) == 0) {
                sourceX = x + 0.5 - disMod + (rand.nextFloat() * (disMod * 2));
                sourceY = y + 0.5 - disMod + (rand.nextFloat() * (disMod * 2));
                sourceZ = z + 0.5 - disMod + (rand.nextFloat() * (disMod * 2));
                Particles.EnergyTransferParticle passiveParticle = reciveEnergy
                        ? new Particles.EnergyTransferParticle(
                                worldObj,
                                targetX,
                                targetY,
                                targetZ,
                                sourceX,
                                sourceY,
                                sourceZ,
                                false)
                        : new Particles.EnergyTransferParticle(
                                worldObj,
                                sourceX,
                                sourceY,
                                sourceZ,
                                targetX,
                                targetY,
                                targetZ,
                                false);
                ParticleHandler.spawnCustomParticle(passiveParticle, 35);
            }
        }
    }

    private boolean isValidStructure() {
        boolean hasGlassOnTop = isGlass(xCoord, yCoord + 1, zCoord);
        boolean hasGlassOnBottom = isGlass(xCoord, yCoord - 1, zCoord);
        return hasGlassOnTop != hasGlassOnBottom;
    }

    private boolean isGlass(int x, int y, int z) {
        return worldObj.getBlock(x, y, z) == ModBlocks.invisibleMultiblock && worldObj.getBlockMetadata(x, y, z) == 2;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        active = compound.getBoolean("Active");
        reciveEnergy = compound.getBoolean("Input");
        int i = compound.getInteger("Cores");
        List<TileLocation> list = new ArrayList<>();
        for (int j = 0; j < i; j++) {
            TileLocation l = new TileLocation();
            l.readFromNBT(compound, "Core" + j);
            list.add(l);
        }
        coreLocations = list;
        selectedCore = compound.getInteger("SelectedCore");
        particleRate = compound.getByte("ParticleRate");
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {

        super.writeToNBT(compound);
        compound.setBoolean("Active", active);
        compound.setBoolean("Input", reciveEnergy);
        int i = coreLocations.size();
        compound.setInteger("Cores", i);
        for (int j = 0; j < i; j++) {
            coreLocations.get(j).writeToNBT(compound, "Core" + j);
        }
        compound.setInteger("SelectedCore", selectedCore);
        compound.setByte("ParticleRate", particleRate);
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound nbttagcompound = new NBTTagCompound();
        writeToNBT(nbttagcompound);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, nbttagcompound);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        readFromNBT(pkt.func_148857_g());
    }

    /* IEnergyHandler */
    @Override
    public boolean canConnectEnergy(ForgeDirection from) {
        return true;
    }

    @Override
    public int receiveEnergy(ForgeDirection from, int maxReceive, boolean simulate) {
        if (getMaster() == null) return 0;
        int received = reciveEnergy ? getMaster().receiveEnergy(maxReceive, simulate) : 0;
        if (!simulate && received > 0) particleRate = (byte) Math.min(20, received < 500 ? 1 : received / 500);
        return received;
    }

    @Override
    public int extractEnergy(ForgeDirection from, int maxExtract, boolean simulate) {
        if (getMaster() == null || !getMaster().isOnline()) return 0;
        int extracted = reciveEnergy ? 0 : getMaster().extractEnergy(maxExtract, simulate);
        if (!simulate && extracted > 0) particleRate = (byte) Math.min(20, extracted < 500 ? 1 : extracted / 500);
        return extracted;
    }

    @Override
    public int getEnergyStored(ForgeDirection from) {
        if (getMaster() == null) return 0;
        return (int) Math.min(Integer.MAX_VALUE, getMaster().getEnergyStored());
    }

    @Override
    public int getMaxEnergyStored(ForgeDirection from) {
        if (getMaster() == null) return 0;
        return (int) Math.min(Integer.MAX_VALUE, getMaster().getMaxEnergyStored());
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }

    private void detectAndSendChanges() {
        if (lastTickActive != active) lastTickActive = (Boolean) sendObjectToClient(
                References.BOOLEAN_ID,
                0,
                active,
                new NetworkRegistry.TargetPoint(worldObj.provider.dimensionId, xCoord, yCoord, zCoord, 256));
        if (lastTickReciveEnergy != reciveEnergy) lastTickReciveEnergy = (Boolean) sendObjectToClient(
                References.BOOLEAN_ID,
                1,
                reciveEnergy,
                new NetworkRegistry.TargetPoint(worldObj.provider.dimensionId, xCoord, yCoord, zCoord, 256));
        if (lastTickParticleRate != particleRate)
            lastTickParticleRate = (Byte) sendObjectToClient(References.BYTE_ID, 2, particleRate);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void receiveObjectFromServer(int index, Object object) {
        switch (index) {
            case 0:
                active = (Boolean) object;
                break;
            case 1:
                reciveEnergy = (Boolean) object;
                break;
            case 2:
                particleRate = (Byte) object;
                break;
        }
    }

    @Override
    public double getEnergyStored() {
        return getMaster() != null ? getMaster().getEnergyStored() : 0D;
    }

    @Override
    public double getMaxEnergyStored() {
        return getMaster() != null ? getMaster().getMaxEnergyStored() : 0D;
    }

    @Override
    public long getExtendedStorage() {
        return getMaster() != null ? getMaster().getEnergyStored() : 0L;
    }

    @Override
    public long getExtendedCapacity() {
        return getMaster() != null ? getMaster().getMaxEnergyStored() : 0L;
    }

    @Override
    public String getName() {
        return "draconic_rf_storage";
    }

    @Override
    public String[] getMethodNames() {
        return new String[] { "getEnergyStored", "getMaxEnergyStored" };
    }

    @Override
    public Object[] callMethod(String method, Object... args) {
        if (method.equals("getEnergyStored")) return new Object[] { getExtendedStorage() };
        else if (method.equals("getMaxEnergyStored")) return new Object[] { getExtendedCapacity() };
        return new Object[0];
    }

    @Override
    @Optional.Method(modid = "gregtech")
    public long injectEnergyUnits(ForgeDirection side, long voltage, long amperage) {
        if (!reciveEnergy) {
            return 0;
        }
        TileEnergyStorageCore core = getMaster();
        if (core == null || !core.isOnline()) {
            return 0;
        }
        long energyReceived = core.receiveElectricEnergy(voltage, amperage);
        if (energyReceived > 0) particleRate = (byte) Math.min(20, energyReceived < 500 ? 1 : energyReceived / 500);
        return (long) Math.ceil((double) energyReceived / voltage);
    }

    private long drainEnergyUnits(long voltage, long amperage) {
        TileEnergyStorageCore core = getMaster();
        if (core == null || !core.isOnline()) {
            return 0;
        }
        long energyExtracted = core.extractElectricEnergy(voltage, amperage);
        if (energyExtracted > 0) particleRate = (byte) Math.min(20, energyExtracted < 500 ? 1 : energyExtracted / 500);
        return (long) Math.ceil((double) energyExtracted / voltage);
    }

    @Override
    @Optional.Method(modid = "gregtech")
    public boolean inputEnergyFrom(ForgeDirection side) {
        if (!reciveEnergy) {
            return false;
        }
        TileEnergyStorageCore core = getMaster();
        return core != null && core.isOnline();
    }

    @Override
    @Optional.Method(modid = "gregtech")
    public boolean outputsEnergyTo(ForgeDirection side) {
        if (reciveEnergy) {
            return false;
        }
        TileEnergyStorageCore core = getMaster();
        return core != null && core.isOnline();
    }

    @Override
    @Optional.Method(modid = "gregtech")
    public byte getColorization() {
        return -1;
    }

    @Override
    @Optional.Method(modid = "gregtech")
    public byte setColorization(byte aColor) {
        return -1;
    }
}
