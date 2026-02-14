package com.brandon3055.draconicevolution.common.tileentities.multiblocktiles;

import static com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil.formatNumber;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.brandon3055.draconicevolution.common.ModBlocks;
import com.brandon3055.draconicevolution.common.blocks.multiblock.InvisibleMultiblock;
import com.brandon3055.draconicevolution.common.blocks.multiblock.MultiblockHelper.TileLocation;
import com.brandon3055.draconicevolution.common.handler.BalanceConfigHandler;
import com.brandon3055.draconicevolution.common.lib.References;
import com.brandon3055.draconicevolution.common.tileentities.TileObjectSync;
import com.brandon3055.draconicevolution.common.tileentities.TileParticleGenerator;
import com.brandon3055.draconicevolution.common.utils.InfoHelper;
import com.brandon3055.draconicevolution.common.utils.LogHelper;

import cpw.mods.fml.common.network.NetworkRegistry;

/**
 * Created by Brandon on 25/07/2014.
 */
public class TileEnergyStorageCore extends TileObjectSync {

    public static final int STABILIZER_SEARCH_DISTANCE = 11;

    @FunctionalInterface
    private interface IBlockAction {

        boolean process(int x, int y, int z);
    }

    protected TileLocation[] stabilizers = new TileLocation[4];
    protected int tier = 0;
    protected boolean online = false;
    public float modelRotation = 0;
    private long energy = 0;
    private long capacity = 0;
    private long lastTickCapacity = 0;

    public TileEnergyStorageCore() {
        for (int i = 0; i < stabilizers.length; i++) {
            stabilizers[i] = new TileLocation();
        }
    }

    @Override
    public void updateEntity() {
        if (!online) {
            return;
        }
        if (worldObj.isRemote) {
            modelRotation += 0.5;
            return;
        }
        detectAndSendChanges();
    }

    /**
     * ######################MultiBlock Methods#######################
     */
    public boolean tryActivate(boolean isCreativeMode) {
        if (!findStabilizers()) {
            return false;
        }
        if (!setTier()) {
            return false;
        }

        online = false;
        boolean isStructureValid = isCreativeMode ? scanStructure(this::setInnerBlock, this::setOuterBlock)
                : scanStructure(this::isInnerBlock, this::isOuterBlock);
        if (isStructureValid) {
            if (scanStructure(this::activateInnerBlock, this::activateOuterBlock)) {
                online = true;
                activateStabilizers();
                worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
                return true;
            }
            deactivateStabilizers();
        }
        return false;
    }

    public void validateStructure(boolean update) {
        online = areStabilizersActive() && scanStructure(this::isInnerBlock, this::isOuterBlock);
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        if (!online) {
            deactivateStabilizers();
            if (update) {
                reIntegrate();
            }
        }
    }

    private void reIntegrate() {
        for (int x = xCoord - 1; x <= xCoord + 1; x++) {
            for (int y = yCoord - 1; y <= yCoord + 1; y++) {
                for (int z = zCoord - 1; z <= zCoord + 1; z++) {
                    if (worldObj.getBlock(x, y, z) == ModBlocks.invisibleMultiblock) {
                        InvisibleMultiblock.revertStructure(worldObj, x, y, z);
                    }
                }
            }
        }
    }

    private boolean findStabilizers() {
        final ForgeDirection[] horizontalDirections = new ForgeDirection[] { ForgeDirection.EAST, ForgeDirection.SOUTH,
                ForgeDirection.WEST, ForgeDirection.NORTH };
        int stabilizersCount = 0;
        for (ForgeDirection direction : horizontalDirections) {
            for (int distance = 1; distance <= STABILIZER_SEARCH_DISTANCE; distance++) {
                int x = xCoord + direction.offsetX * distance;
                int z = zCoord + direction.offsetZ * distance;
                if (worldObj.getBlock(x, yCoord, z) == ModBlocks.particleGenerator) {
                    if (worldObj.getBlockMetadata(x, yCoord, z) == 1) {
                        return false;
                    }
                    stabilizers[stabilizersCount++] = new TileLocation(x, yCoord, z);
                    break;
                }
            }
        }
        return stabilizersCount == 4;
    }

    private boolean setTier() {
        final int range = 5;

        int[] outerBlocksDistances = new int[6];
        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            for (int distance = 1; distance <= range; distance++) {
                int x = xCoord + direction.offsetX * distance;
                int y = yCoord + direction.offsetY * distance;
                int z = zCoord + direction.offsetZ * distance;
                if (isOuterBlock(x, y, z)) {
                    outerBlocksDistances[direction.ordinal()] = distance;
                    break;
                }
            }
        }
        for (int i = 1; i < outerBlocksDistances.length; i++) {
            if (outerBlocksDistances[i] != outerBlocksDistances[0]) {
                return false;
            }
        }

        tier = outerBlocksDistances[0];
        if (tier > 1) {
            tier++;
        }
        if (tier == 1) {
            if (isOuterBlock(xCoord + 1, yCoord + 1, zCoord)) {
                tier = 2;
            }
        }
        return true;
    }

    private boolean scanStructure(IBlockAction innerBlock, IBlockAction outerBlock) {
        final IBlockAction isReplaceable = this::isReplaceable;
        return switch (tier) {
            case 0 -> scanCube(isReplaceable, 1);
            case 1 -> scanRings(isReplaceable, 1, 1) && scanSides(outerBlock, 0, 1);
            case 2 -> scanCube(outerBlock, 1);
            case 3 -> scanCube(innerBlock, 1) && scanSides(outerBlock, 1, 2);
            case 4 -> scanCube(innerBlock, 1) && scanSides(innerBlock, 1, 2)
                    && scanSides(outerBlock, 1, 3)
                    && scanRings(outerBlock, 2, 2);
            case 5 -> scanCube(innerBlock, 2) && scanSides(innerBlock, 1, 3)
                    && scanSides(outerBlock, 1, 4)
                    && scanRings(outerBlock, 2, 3);
            case 6 -> scanCube(innerBlock, 2) && scanSides(innerBlock, 2, 3)
                    && scanSides(innerBlock, 1, 4)
                    && scanSides(outerBlock, 1, 5)
                    && scanRings(outerBlock, 2, 4)
                    && scanRings(outerBlock, 3, 3);
            default -> false;
        };
    }

    private boolean scanCube(IBlockAction action, int size) {
        for (int x = xCoord - size; x <= xCoord + size; x++) {
            for (int y = yCoord - size; y <= yCoord + size; y++) {
                for (int z = zCoord - size; z <= zCoord + size; z++) {
                    if (x == xCoord && y == yCoord && z == zCoord) {
                        continue;
                    }
                    if (!action.process(x, y, z)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean scanRings(IBlockAction action, int radius, int distance) {
        final boolean isAsymmetric = radius != distance;
        for (int x = xCoord - radius; x <= xCoord + radius; x++) {
            boolean isSuccessful = action.process(x, yCoord - radius, zCoord - distance)
                    && action.process(x, yCoord - radius, zCoord + distance)
                    && action.process(x, yCoord + radius, zCoord - distance)
                    && action.process(x, yCoord + radius, zCoord + distance);
            if (isSuccessful && isAsymmetric) {
                isSuccessful = action.process(x, yCoord - distance, zCoord - radius)
                        && action.process(x, yCoord - distance, zCoord + radius)
                        && action.process(x, yCoord + distance, zCoord - radius)
                        && action.process(x, yCoord + distance, zCoord + radius);
            }
            if (!isSuccessful) {
                return false;
            }
        }
        for (int y = yCoord - radius; y <= yCoord + radius; y++) {
            boolean isSuccessful = action.process(xCoord - radius, y, zCoord - distance)
                    && action.process(xCoord - radius, y, zCoord + distance)
                    && action.process(xCoord + radius, y, zCoord - distance)
                    && action.process(xCoord + radius, y, zCoord + distance);
            if (isSuccessful && isAsymmetric) {
                isSuccessful = action.process(xCoord - distance, y, zCoord - radius)
                        && action.process(xCoord - distance, y, zCoord + radius)
                        && action.process(xCoord + distance, y, zCoord - radius)
                        && action.process(xCoord + distance, y, zCoord + radius);
            }
            if (!isSuccessful) {
                return false;
            }
        }
        for (int z = zCoord - radius; z <= zCoord + radius; z++) {
            boolean isSuccessful = action.process(xCoord - radius, yCoord - distance, z)
                    && action.process(xCoord - radius, yCoord + distance, z)
                    && action.process(xCoord + radius, yCoord - distance, z)
                    && action.process(xCoord + radius, yCoord + distance, z);
            if (isSuccessful && isAsymmetric) {
                isSuccessful = action.process(xCoord - distance, yCoord - radius, z)
                        && action.process(xCoord - distance, yCoord + radius, z)
                        && action.process(xCoord + distance, yCoord - radius, z)
                        && action.process(xCoord + distance, yCoord + radius, z);
            }
            if (!isSuccessful) {
                return false;
            }
        }
        return true;
    }

    private boolean scanSides(IBlockAction action, int size, int distance) {
        for (int x = xCoord - size; x <= xCoord + size; x++) {
            for (int y = yCoord - size; y <= yCoord + size; y++) {
                boolean isSuccessful = action.process(x, y, zCoord - distance)
                        && action.process(x, y, zCoord + distance);
                if (!isSuccessful) {
                    return false;
                }
            }
        }
        for (int x = xCoord - size; x <= xCoord + size; x++) {
            for (int z = zCoord - size; z <= zCoord + size; z++) {
                boolean isSuccessful = action.process(x, yCoord - distance, z)
                        && action.process(x, yCoord + distance, z);
                if (!isSuccessful) {
                    return false;
                }
            }
        }
        for (int y = yCoord - size; y <= yCoord + size; y++) {
            for (int z = zCoord - size; z <= zCoord + size; z++) {
                boolean isSuccessful = action.process(xCoord - distance, y, z)
                        && action.process(xCoord + distance, y, z);
                if (!isSuccessful) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isReplaceable(int x, int y, int z) {
        return worldObj.isAirBlock(x, y, z) || worldObj.getBlock(x, y, z).isReplaceable(worldObj, x, y, z);
    }

    private boolean isInnerBlock(int x, int y, int z) {
        Block block = worldObj.getBlock(x, y, z);
        int metadata = worldObj.getBlockMetadata(x, y, z);
        return (block == BalanceConfigHandler.energyStorageStructureBlock
                && metadata == BalanceConfigHandler.energyStorageStructureBlockMetadata)
                || (block == ModBlocks.invisibleMultiblock && metadata == 1);
    }

    private boolean activateInnerBlock(int x, int y, int z) {
        worldObj.setBlock(x, y, z, ModBlocks.invisibleMultiblock, 1, 2);
        TileEntity tile = worldObj.getTileEntity(x, y, z);
        if (tile instanceof TileInvisibleMultiblock multiblock) {
            multiblock.master = new TileLocation(xCoord, yCoord, zCoord);
            return true;
        }
        LogHelper.error("Failed to activate structure (activateRedstone)");
        return false;
    }

    private boolean setInnerBlock(int x, int y, int z) {
        worldObj.setBlock(
                x,
                y,
                z,
                BalanceConfigHandler.energyStorageStructureBlock,
                BalanceConfigHandler.energyStorageStructureBlockMetadata,
                3);
        return true;
    }

    private boolean isOuterBlock(int x, int y, int z) {
        Block block = worldObj.getBlock(x, y, z);
        int metadata = worldObj.getBlockMetadata(x, y, z);
        return (block == BalanceConfigHandler.energyStorageStructureOuterBlock
                && metadata == BalanceConfigHandler.energyStorageStructureOuterBlockMetadata)
                || (block == ModBlocks.invisibleMultiblock && metadata == 0);
    }

    private boolean activateOuterBlock(int x, int y, int z) {
        worldObj.setBlock(x, y, z, ModBlocks.invisibleMultiblock, 0, 2);
        TileEntity tile = worldObj.getTileEntity(x, y, z);
        if (tile instanceof TileInvisibleMultiblock multiblock) {
            multiblock.master = new TileLocation(xCoord, yCoord, zCoord);
            return true;
        }
        LogHelper.error("Failed to activate structure (activateDraconium)");
        return false;
    }

    private boolean setOuterBlock(int x, int y, int z) {
        worldObj.setBlock(
                x,
                y,
                z,
                BalanceConfigHandler.energyStorageStructureOuterBlock,
                BalanceConfigHandler.energyStorageStructureOuterBlockMetadata,
                3);
        return true;
    }

    public boolean isOnline() {
        return online;
    }

    private void activateStabilizers() {
        for (TileLocation stabilizer : stabilizers) {
            TileEntity tile = stabilizer.getTileEntity(worldObj);
            if (!(tile instanceof TileParticleGenerator particleGenerator)) {
                LogHelper.error("Missing Tile Entity (Particle Generator)");
                return;
            }
            particleGenerator.isInStabilizerMode = true;
            particleGenerator.setMaster(new TileLocation(xCoord, yCoord, zCoord));
            worldObj.setBlockMetadataWithNotify(
                    stabilizer.getXCoord(),
                    stabilizer.getYCoord(),
                    stabilizer.getZCoord(),
                    1,
                    2);
        }
        initializeCapacity();
    }

    private void initializeCapacity() {
        long capacity = switch (tier) {
            case 0 -> BalanceConfigHandler.energyStorageTier1Storage;
            case 1 -> BalanceConfigHandler.energyStorageTier2Storage;
            case 2 -> BalanceConfigHandler.energyStorageTier3Storage;
            case 3 -> BalanceConfigHandler.energyStorageTier4Storage;
            case 4 -> BalanceConfigHandler.energyStorageTier5Storage;
            case 5 -> BalanceConfigHandler.energyStorageTier6Storage;
            case 6 -> BalanceConfigHandler.energyStorageTier7Storage;
            default -> 0;
        };
        this.capacity = capacity;
        if (energy > capacity) energy = capacity;
    }

    public void deactivateStabilizers() {
        for (TileLocation stabilizer : stabilizers) {
            TileEntity tile = stabilizer.getTileEntity(worldObj);
            if (tile instanceof TileParticleGenerator particleGenerator) {
                particleGenerator.isInStabilizerMode = false;
                worldObj.setBlockMetadataWithNotify(
                        stabilizer.getXCoord(),
                        stabilizer.getYCoord(),
                        stabilizer.getZCoord(),
                        0,
                        2);
            }
        }
    }

    private boolean areStabilizersActive() {
        for (TileLocation stabilizer : stabilizers) {
            TileEntity tile = stabilizer.getTileEntity(worldObj);
            if (!(tile instanceof TileParticleGenerator particleGenerator)) {
                return false;
            }
            if (!particleGenerator.isInStabilizerMode || stabilizer.getBlockMetadata(worldObj) != 1) {
                return false;
            }
            TileEnergyStorageCore core = particleGenerator.getMaster();
            if (core == null || core.xCoord != xCoord || core.yCoord != yCoord || core.zCoord != zCoord) {
                return false;
            }
        }
        return true;
    }

    public int getTier() {
        return tier;
    }

    /**
     * ###############################################################
     */
    @Override
    public void writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setBoolean("Online", online);
        compound.setShort("Tier", (short) tier);
        compound.setLong("EnergyL", energy);
        for (int i = 0; i < stabilizers.length; i++) {
            stabilizers[i].writeToNBT(compound, String.valueOf(i));
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        online = compound.getBoolean("Online");
        tier = compound.getShort("Tier");
        energy = compound.getLong("EnergyL");
        if (compound.hasKey("Energy")) energy = (long) compound.getDouble("Energy");
        for (int i = 0; i < stabilizers.length; i++) {
            stabilizers[i].readFromNBT(compound, String.valueOf(i));
        }
        initializeCapacity();
        super.readFromNBT(compound);
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

    /* EnergyHandler */

    public int receiveEnergy(int maxReceive, boolean simulate) {
        long energyReceived = Math.min(capacity - energy, maxReceive);

        if (!simulate) {
            energy += energyReceived;
        }
        return (int) energyReceived;
    }

    public int extractEnergy(int maxExtract, boolean simulate) {
        long energyExtracted = Math.min(energy, maxExtract);

        if (!simulate) {
            energy -= energyExtracted;
        }
        return (int) energyExtracted;
    }

    public long getEnergyStored() {
        return energy;
    }

    public long getMaxEnergyStored() {
        return capacity;
    }

    public List<String> getDisplayInformation(boolean shouldShowName) {
        List<String> information = new ArrayList<>();
        if (shouldShowName) {
            information.add(InfoHelper.HITC() + ModBlocks.energyStorageCore.getLocalizedName());
        }
        information.add(StatCollector.translateToLocal("info.de.tier.txt") + ": " + InfoHelper.ITC() + (tier + 1));
        information.add(
                StatCollector.translateToLocal("info.de.charge.txt") + ": "
                        + InfoHelper.ITC()
                        + formatNumber(energy)
                        + " / "
                        + formatNumber(capacity)
                        + " ["
                        + formatNumber(energy)
                        + " RF]");
        return information;
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }

    @Override
    public double getMaxRenderDistanceSquared() {
        return 40960.0D;
    }

    private void detectAndSendChanges() {
        if (lastTickCapacity != energy) {
            lastTickCapacity = (long) sendObjectToClient(
                    References.LONG_ID,
                    0,
                    energy,
                    new NetworkRegistry.TargetPoint(worldObj.provider.dimensionId, xCoord, yCoord, zCoord, 20));
        }
    }

    @Override
    public void receiveObjectFromServer(int index, Object object) {
        energy = (Long) object;
    }
}
