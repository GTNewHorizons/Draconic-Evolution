package com.brandon3055.draconicevolution.common.tileentities.multiblocktiles;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.StatCollector;

import com.brandon3055.brandonscore.common.utills.InfoHelper;
import com.brandon3055.brandonscore.common.utills.Utills;
import com.brandon3055.draconicevolution.common.ModBlocks;
import com.brandon3055.draconicevolution.common.blocks.multiblock.MultiblockHelper.TileLocation;
import com.brandon3055.draconicevolution.common.handler.BalanceConfigHandler;
import com.brandon3055.draconicevolution.common.lib.References;
import com.brandon3055.draconicevolution.common.tileentities.TileObjectSync;
import com.brandon3055.draconicevolution.common.tileentities.TileParticleGenerator;
import com.brandon3055.draconicevolution.common.utills.LogHelper;

import cpw.mods.fml.common.network.NetworkRegistry;

/**
 * Created by Brandon on 25/07/2014.
 */
public class TileEnergyStorageCore extends TileObjectSync {

    private enum MultiblockPartType {
        AIR,
        REDSTONE,
        DRACONIUM
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
        if (!online) return;
        if (worldObj.isRemote) {
            modelRotation += 0.5;
            return;
        }
        detectAndRendChanges();
    }

    /**
     * ######################MultiBlock Methods#######################
     */
    public boolean tryActivate() {
        if (!findStabilizers()) return false;
        if (!setTier(false)) return false;
        if (!testOrActivateStructureIfValid(false, false)) return false;
        online = true;
        if (!testOrActivateStructureIfValid(false, true)) {
            online = false;
            deactivateStabilizers();
            return false;
        }
        activateStabilizers();
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        return true;
    }

    public boolean creativeActivate() {
        if (!findStabilizers()) return false;
        if (!setTier(false)) return false;
        if (!testOrActivateStructureIfValid(true, false)) return false;
        online = true;
        if (!testOrActivateStructureIfValid(false, true)) {
            online = false;
            deactivateStabilizers();
            return false;
        }
        activateStabilizers();
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        return true;
    }

    public boolean isStructureStillValid(boolean update) {
        if (!checkStabilizers()) online = false;
        if (!testOrActivateStructureIfValid(false, false)) online = false;
        if (!areStabilizersActive()) online = false;
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        if (!online) deactivateStabilizers();
        if (update && !online) reIntegrate();
        return online;
    }

    private void reIntegrate() {
        for (int x = xCoord - 1; x <= xCoord + 1; x++) {
            for (int y = yCoord - 1; y <= yCoord + 1; y++) {
                for (int z = zCoord - 1; z <= zCoord + 1; z++) {
                    if (worldObj.getBlock(x, y, z) == ModBlocks.invisibleMultiblock) {
                        if (worldObj.getBlockMetadata(x, y, z) == 0) {
                            worldObj.setBlock(
                                    x,
                                    y,
                                    z,
                                    BalanceConfigHandler.energyStorageStructureOuterBlock,
                                    BalanceConfigHandler.energyStorageStructureOuterBlockMetadata,
                                    3);
                        } else if (worldObj.getBlockMetadata(x, y, z) == 1) {
                            worldObj.setBlock(
                                    x,
                                    y,
                                    z,
                                    BalanceConfigHandler.energyStorageStructureBlock,
                                    BalanceConfigHandler.energyStorageStructureBlockMetadata,
                                    3);
                        }
                    }
                }
            }
        }
    }

    private boolean findStabilizers() {
        boolean flag = true;
        for (int x = xCoord; x <= xCoord + 11; x++) {
            if (worldObj.getBlock(x, yCoord, zCoord) == ModBlocks.particleGenerator) {
                if (worldObj.getBlockMetadata(x, yCoord, zCoord) == 1) {
                    flag = false;
                    break;
                }
                stabilizers[0] = new TileLocation(x, yCoord, zCoord);
                break;
            } else if (x == xCoord + 11) {
                flag = false;
            }
        }
        for (int x = xCoord; x >= xCoord - 11; x--) {
            if (worldObj.getBlock(x, yCoord, zCoord) == ModBlocks.particleGenerator) {
                if (worldObj.getBlockMetadata(x, yCoord, zCoord) == 1) {
                    flag = false;
                    break;
                }
                stabilizers[1] = new TileLocation(x, yCoord, zCoord);
                break;
            } else if (x == xCoord - 11) {
                flag = false;
            }
        }
        for (int z = zCoord; z <= zCoord + 11; z++) {
            if (worldObj.getBlock(xCoord, yCoord, z) == ModBlocks.particleGenerator) {
                if (worldObj.getBlockMetadata(xCoord, yCoord, z) == 1) {
                    flag = false;
                    break;
                }
                stabilizers[2] = new TileLocation(xCoord, yCoord, z);
                break;
            } else if (z == zCoord + 11) {
                flag = false;
            }
        }
        for (int z = zCoord; z >= zCoord - 11; z--) {
            if (worldObj.getBlock(xCoord, yCoord, z) == ModBlocks.particleGenerator) {
                if (worldObj.getBlockMetadata(xCoord, yCoord, z) == 1) {
                    flag = false;
                    break;
                }
                stabilizers[3] = new TileLocation(xCoord, yCoord, z);
                break;
            } else if (z == zCoord - 11) {
                flag = false;
            }
        }
        return flag;
    }

    private boolean setTier(boolean force) {
        if (force) return true;
        int xPos = 0;
        int xNeg = 0;
        int yPos = 0;
        int yNeg = 0;
        int zPos = 0;
        int zNeg = 0;
        int range = 5;

        for (int x = 0; x <= range; x++) {
            if (testForOrActivateDraconium(xCoord + x, yCoord, zCoord, false, false)) {
                xPos = x;
                break;
            }
        }

        for (int x = 0; x <= range; x++) {
            if (testForOrActivateDraconium(xCoord - x, yCoord, zCoord, false, false)) {
                xNeg = x;
                break;
            }
        }

        for (int y = 0; y <= range; y++) {
            if (testForOrActivateDraconium(xCoord, yCoord + y, zCoord, false, false)) {
                yPos = y;
                break;
            }
        }

        for (int y = 0; y <= range; y++) {
            if (testForOrActivateDraconium(xCoord, yCoord - y, zCoord, false, false)) {
                yNeg = y;
                break;
            }
        }

        for (int z = 0; z <= range; z++) {
            if (testForOrActivateDraconium(xCoord, yCoord, zCoord + z, false, false)) {
                zPos = z;
                break;
            }
        }

        for (int z = 0; z <= range; z++) {
            if (testForOrActivateDraconium(xCoord, yCoord, zCoord - z, false, false)) {
                zNeg = z;
                break;
            }
        }

        if (zNeg != zPos || zNeg != yNeg || zNeg != yPos || zNeg != xNeg || zNeg != xPos) return false;

        tier = xPos;
        if (tier > 1) tier++;
        if (tier == 1) {
            if (testForOrActivateDraconium(xCoord + 1, yCoord + 1, zCoord, false, false)) tier = 2;
        }
        return true;
    }

    private boolean testOrActivateStructureIfValid(boolean setBlocks, boolean activate) {
        switch (tier) {
            case 0:
                if (!testOrActivateRect(1, 1, 1, MultiblockPartType.AIR, setBlocks, activate)) return false;
                break;
            case 1:
                if (!testForOrActivateDraconium(xCoord + 1, yCoord, zCoord, setBlocks, activate)
                        || !testForOrActivateDraconium(xCoord - 1, yCoord, zCoord, setBlocks, activate)
                        || !testForOrActivateDraconium(xCoord, yCoord + 1, zCoord, setBlocks, activate)
                        || !testForOrActivateDraconium(xCoord, yCoord - 1, zCoord, setBlocks, activate)
                        || !testForOrActivateDraconium(xCoord, yCoord, zCoord + 1, setBlocks, activate)
                        || !testForOrActivateDraconium(xCoord, yCoord, zCoord - 1, setBlocks, activate))
                    return false;
                if (!isReplaceable(xCoord + 1, yCoord + 1, zCoord, setBlocks)
                        || !isReplaceable(xCoord, yCoord + 1, zCoord + 1, setBlocks)
                        || !isReplaceable(xCoord - 1, yCoord + 1, zCoord, setBlocks)
                        || !isReplaceable(xCoord, yCoord + 1, zCoord - 1, setBlocks)
                        || !isReplaceable(xCoord + 1, yCoord - 1, zCoord, setBlocks)
                        || !isReplaceable(xCoord, yCoord - 1, zCoord + 1, setBlocks)
                        || !isReplaceable(xCoord - 1, yCoord - 1, zCoord, setBlocks)
                        || !isReplaceable(xCoord, yCoord - 1, zCoord - 1, setBlocks)
                        || !isReplaceable(xCoord + 1, yCoord, zCoord + 1, setBlocks)
                        || !isReplaceable(xCoord - 1, yCoord, zCoord - 1, setBlocks)
                        || !isReplaceable(xCoord + 1, yCoord, zCoord - 1, setBlocks)
                        || !isReplaceable(xCoord - 1, yCoord, zCoord + 1, setBlocks))
                    return false;
                if (!isReplaceable(xCoord + 1, yCoord + 1, zCoord + 1, setBlocks)
                        || !isReplaceable(xCoord - 1, yCoord + 1, zCoord - 1, setBlocks)
                        || !isReplaceable(xCoord + 1, yCoord + 1, zCoord - 1, setBlocks)
                        || !isReplaceable(xCoord - 1, yCoord + 1, zCoord + 1, setBlocks)
                        || !isReplaceable(xCoord + 1, yCoord - 1, zCoord + 1, setBlocks)
                        || !isReplaceable(xCoord - 1, yCoord - 1, zCoord - 1, setBlocks)
                        || !isReplaceable(xCoord + 1, yCoord - 1, zCoord - 1, setBlocks)
                        || !isReplaceable(xCoord - 1, yCoord - 1, zCoord + 1, setBlocks))
                    return false;
                break;
            case 2:
                if (!testOrActivateRect(1, 1, 1, MultiblockPartType.DRACONIUM, setBlocks, activate)) return false;
                break;
            case 3:
                if (!testOrActivateSides(1, MultiblockPartType.DRACONIUM, setBlocks, activate)) return false;
                if (!testOrActivateRect(1, 1, 1, MultiblockPartType.REDSTONE, setBlocks, activate)) return false;
                break;
            case 4:
                if (!testOrActivateSides(2, MultiblockPartType.DRACONIUM, setBlocks, activate)) return false;
                if (!testOrActivateRect(2, 1, 1, MultiblockPartType.REDSTONE, setBlocks, activate)) return false;
                if (!testOrActivateRect(1, 2, 1, MultiblockPartType.REDSTONE, setBlocks, activate)) return false;
                if (!testOrActivateRect(1, 1, 2, MultiblockPartType.REDSTONE, setBlocks, activate)) return false;
                if (!testOrActivateRings(2, 2, MultiblockPartType.DRACONIUM, setBlocks, activate)) return false;
                break;
            case 5:
                if (!testOrActivateSides(3, MultiblockPartType.DRACONIUM, setBlocks, activate)) return false;
                if (!testOrActivateSides(2, MultiblockPartType.REDSTONE, setBlocks, activate)) return false;
                if (!testOrActivateRect(2, 2, 2, MultiblockPartType.REDSTONE, setBlocks, activate)) return false;
                if (!testOrActivateRings(2, 3, MultiblockPartType.DRACONIUM, setBlocks, activate)) return false;
                break;
            case 6:
                if (!testOrActivateSides(4, MultiblockPartType.DRACONIUM, setBlocks, activate)) return false;
                if (!testOrActivateSides(3, MultiblockPartType.REDSTONE, setBlocks, activate)) return false;
                if (!testOrActivateRect(3, 2, 2, MultiblockPartType.REDSTONE, setBlocks, activate)) return false;
                if (!testOrActivateRect(2, 3, 2, MultiblockPartType.REDSTONE, setBlocks, activate)) return false;
                if (!testOrActivateRect(2, 2, 3, MultiblockPartType.REDSTONE, setBlocks, activate)) return false;
                if (!testOrActivateRings(2, 4, MultiblockPartType.DRACONIUM, setBlocks, activate)) return false;
                if (!testOrActivateRings(3, 3, MultiblockPartType.DRACONIUM, setBlocks, activate)) return false;
                break;
        }
        return true;
    }

    private boolean testOrActivateRect(int xDim, int yDim, int zDim, MultiblockPartType block, boolean set,
            boolean activate) {
        for (int x = xCoord - xDim; x <= xCoord + xDim; x++) {
            for (int y = yCoord - yDim; y <= yCoord + yDim; y++) {
                for (int z = zCoord - zDim; z <= zCoord + zDim; z++) {

                    switch (block) {
                        case AIR:
                            if (!(x == xCoord && y == yCoord && z == zCoord) && !isReplaceable(x, y, z, set))
                                return false;
                            break;
                        case REDSTONE:
                            if (!(x == xCoord && y == yCoord && z == zCoord)
                                    && !testForOrActivateRedstone(x, y, z, set, activate))
                                return false;
                            break;
                        case DRACONIUM:
                            if (!(x == xCoord && y == yCoord && z == zCoord)
                                    && !testForOrActivateDraconium(x, y, z, set, activate))
                                return false;
                            break;
                        default:
                            LogHelper.error("Invalid String In Multiblock Structure Code!!!");
                            return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean testOrActivateRings(int size, int dist, MultiblockPartType block, boolean set, boolean activate) {
        for (int y = yCoord - size; y <= yCoord + size; y++) {
            for (int z = zCoord - size; z <= zCoord + size; z++) {

                if (y == yCoord - size || y == yCoord + size || z == zCoord - size || z == zCoord + size) {
                    switch (block) {
                        case AIR:
                            if (!(xCoord + dist == xCoord && y == yCoord && z == zCoord)
                                    && !isReplaceable(xCoord + dist, y, z, set))
                                return false;
                            break;
                        case REDSTONE:
                            if (!(xCoord + dist == xCoord && y == yCoord && z == zCoord)
                                    && !testForOrActivateRedstone(xCoord + dist, y, z, set, activate))
                                return false;
                            break;
                        case DRACONIUM:
                            if (!(xCoord + dist == xCoord && y == yCoord && z == zCoord)
                                    && !testForOrActivateDraconium(xCoord + dist, y, z, set, activate))
                                return false;
                            break;
                        default:
                            LogHelper.error("Invalid String In Multiblock Structure Code!!!");
                            return false;
                    }
                }
            }
        }
        for (int y = yCoord - size; y <= yCoord + size; y++) {
            for (int z = zCoord - size; z <= zCoord + size; z++) {

                if (y == yCoord - size || y == yCoord + size || z == zCoord - size || z == zCoord + size) {
                    switch (block) {
                        case AIR:
                            if (!(xCoord - dist == xCoord && y == yCoord && z == zCoord)
                                    && !isReplaceable(xCoord - dist, y, z, set))
                                return false;
                            break;
                        case REDSTONE:
                            if (!(xCoord - dist == xCoord && y == yCoord && z == zCoord)
                                    && !testForOrActivateRedstone(xCoord - dist, y, z, set, activate))
                                return false;
                            break;
                        case DRACONIUM:
                            if (!(xCoord - dist == xCoord && y == yCoord && z == zCoord)
                                    && !testForOrActivateDraconium(xCoord - dist, y, z, set, activate))
                                return false;
                            break;
                        default:
                            LogHelper.error("Invalid String In Multiblock Structure Code!!!");
                            return false;
                    }
                }
            }
        }

        for (int x = xCoord - size; x <= xCoord + size; x++) {
            for (int z = zCoord - size; z <= zCoord + size; z++) {

                if (x == xCoord - size || x == xCoord + size || z == zCoord - size || z == zCoord + size) {
                    switch (block) {
                        case AIR:
                            if (!(x == xCoord && yCoord + dist == yCoord && z == zCoord)
                                    && !isReplaceable(x, yCoord + dist, z, set))
                                return false;
                            break;
                        case REDSTONE:
                            if (!(x == xCoord && yCoord + dist == yCoord && z == zCoord)
                                    && !testForOrActivateRedstone(x, yCoord + dist, z, set, activate))
                                return false;
                            break;
                        case DRACONIUM:
                            if (!(x == xCoord && yCoord + dist == yCoord && z == zCoord)
                                    && !testForOrActivateDraconium(x, yCoord + dist, z, set, activate))
                                return false;
                            break;
                        default:
                            LogHelper.error("Invalid String In Multiblock Structure Code!!!");
                            return false;
                    }
                }
            }
        }
        for (int x = xCoord - size; x <= xCoord + size; x++) {
            for (int z = zCoord - size; z <= zCoord + size; z++) {

                if (x == xCoord - size || x == xCoord + size || z == zCoord - size || z == zCoord + size) {
                    switch (block) {
                        case AIR:
                            if (!(x == xCoord && yCoord - dist == yCoord && z == zCoord)
                                    && !isReplaceable(x, yCoord - dist, z, set))
                                return false;
                            break;
                        case REDSTONE:
                            if (!(x == xCoord && yCoord - dist == yCoord && z == zCoord)
                                    && !testForOrActivateRedstone(x, yCoord - dist, z, set, activate))
                                return false;
                            break;
                        case DRACONIUM:
                            if (!(x == xCoord && yCoord - dist == yCoord && z == zCoord)
                                    && !testForOrActivateDraconium(x, yCoord - dist, z, set, activate))
                                return false;
                            break;
                        default:
                            LogHelper.error("Invalid String In Multiblock Structure Code!!!");
                            return false;
                    }
                }
            }
        }

        for (int y = yCoord - size; y <= yCoord + size; y++) {
            for (int x = xCoord - size; x <= xCoord + size; x++) {

                if (y == yCoord - size || y == yCoord + size || x == xCoord - size || x == xCoord + size) {
                    switch (block) {
                        case AIR:
                            if (!(x == xCoord && y == yCoord && zCoord + dist == zCoord)
                                    && !isReplaceable(x, y, zCoord + dist, set))
                                return false;
                            break;
                        case REDSTONE:
                            if (!(x == xCoord && y == yCoord && zCoord + dist == zCoord)
                                    && !testForOrActivateRedstone(x, y, zCoord + dist, set, activate))
                                return false;
                            break;
                        case DRACONIUM:
                            if (!(x == xCoord && y == yCoord && zCoord + dist == zCoord)
                                    && !testForOrActivateDraconium(x, y, zCoord + dist, set, activate))
                                return false;
                            break;
                        default:
                            LogHelper.error("Invalid String In Multiblock Structure Code!!!");
                            return false;
                    }
                }
            }
        }
        for (int y = yCoord - size; y <= yCoord + size; y++) {
            for (int x = xCoord - size; x <= xCoord + size; x++) {

                if (y == yCoord - size || y == yCoord + size || x == xCoord - size || x == xCoord + size) {
                    switch (block) {
                        case AIR:
                            if (!(x == xCoord && y == yCoord && zCoord - dist == zCoord)
                                    && !isReplaceable(x, y, zCoord - dist, set))
                                return false;
                            break;
                        case REDSTONE:
                            if (!(x == xCoord && y == yCoord && zCoord - dist == zCoord)
                                    && !testForOrActivateRedstone(x, y, zCoord - dist, set, activate))
                                return false;
                            break;
                        case DRACONIUM:
                            if (!(x == xCoord && y == yCoord && zCoord - dist == zCoord)
                                    && !testForOrActivateDraconium(x, y, zCoord - dist, set, activate))
                                return false;
                            break;
                        default:
                            LogHelper.error("Invalid String In Multiblock Structure Code!!!");
                            return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean testOrActivateSides(int dist, MultiblockPartType block, boolean set, boolean activate) {
        dist++;
        for (int y = yCoord - 1; y <= yCoord + 1; y++) {
            for (int z = zCoord - 1; z <= zCoord + 1; z++) {

                switch (block) {
                    case AIR:
                        if (!(xCoord + dist == xCoord && y == yCoord && z == zCoord)
                                && !isReplaceable(xCoord + dist, y, z, set))
                            return false;
                        break;
                    case REDSTONE:
                        if (!(xCoord + dist == xCoord && y == yCoord && z == zCoord)
                                && !testForOrActivateRedstone(xCoord + dist, y, z, set, activate))
                            return false;
                        break;
                    case DRACONIUM:
                        if (!(xCoord + dist == xCoord && y == yCoord && z == zCoord)
                                && !testForOrActivateDraconium(xCoord + dist, y, z, set, activate))
                            return false;
                        break;
                    default:
                        LogHelper.error("Invalid String In Multiblock Structure Code!!!");
                        return false;
                }
            }
        }
        for (int y = yCoord - 1; y <= yCoord + 1; y++) {
            for (int z = zCoord - 1; z <= zCoord + 1; z++) {

                switch (block) {
                    case AIR:
                        if (!(xCoord - dist == xCoord && y == yCoord && z == zCoord)
                                && !isReplaceable(xCoord - dist, y, z, set))
                            return false;
                        break;
                    case REDSTONE:
                        if (!(xCoord - dist == xCoord && y == yCoord && z == zCoord)
                                && !testForOrActivateRedstone(xCoord - dist, y, z, set, activate))
                            return false;
                        break;
                    case DRACONIUM:
                        if (!(xCoord - dist == xCoord && y == yCoord && z == zCoord)
                                && !testForOrActivateDraconium(xCoord - dist, y, z, set, activate))
                            return false;
                        break;
                    default:
                        LogHelper.error("Invalid String In Multiblock Structure Code!!!");
                        return false;
                }
            }
        }

        for (int x = xCoord - 1; x <= xCoord + 1; x++) {
            for (int z = zCoord - 1; z <= zCoord + 1; z++) {

                switch (block) {
                    case AIR:
                        if (!(x == xCoord && yCoord + dist == yCoord && z == zCoord)
                                && !isReplaceable(x, yCoord + dist, z, set))
                            return false;
                        break;
                    case REDSTONE:
                        if (!(x == xCoord && yCoord + dist == yCoord && z == zCoord)
                                && !testForOrActivateRedstone(x, yCoord + dist, z, set, activate))
                            return false;
                        break;
                    case DRACONIUM:
                        if (!(x == xCoord && yCoord + dist == yCoord && z == zCoord)
                                && !testForOrActivateDraconium(x, yCoord + dist, z, set, activate))
                            return false;
                        break;
                    default:
                        LogHelper.error("Invalid String In Multiblock Structure Code!!!");
                        return false;
                }
            }
        }
        for (int x = xCoord - 1; x <= xCoord + 1; x++) {
            for (int z = zCoord - 1; z <= zCoord + 1; z++) {

                switch (block) {
                    case AIR:
                        if (!(x == xCoord && yCoord - dist == yCoord && z == zCoord)
                                && !isReplaceable(x, yCoord - dist, z, set))
                            return false;
                        break;
                    case REDSTONE:
                        if (!(x == xCoord && yCoord - dist == yCoord && z == zCoord)
                                && !testForOrActivateRedstone(x, yCoord - dist, z, set, activate))
                            return false;
                        break;
                    case DRACONIUM:
                        if (!(x == xCoord && yCoord - dist == yCoord && z == zCoord)
                                && !testForOrActivateDraconium(x, yCoord - dist, z, set, activate))
                            return false;
                        break;
                    default:
                        LogHelper.error("Invalid String In Multiblock Structure Code!!!");
                        return false;
                }
            }
        }

        for (int y = yCoord - 1; y <= yCoord + 1; y++) {
            for (int x = xCoord - 1; x <= xCoord + 1; x++) {

                switch (block) {
                    case AIR:
                        if (!(x == xCoord && y == yCoord && zCoord + dist == zCoord)
                                && !isReplaceable(x, y, zCoord + dist, set))
                            return false;
                        break;
                    case REDSTONE:
                        if (!(x == xCoord && y == yCoord && zCoord + dist == zCoord)
                                && !testForOrActivateRedstone(x, y, zCoord + dist, set, activate))
                            return false;
                        break;
                    case DRACONIUM:
                        if (!(x == xCoord && y == yCoord && zCoord + dist == zCoord)
                                && !testForOrActivateDraconium(x, y, zCoord + dist, set, activate))
                            return false;
                        break;
                    default:
                        LogHelper.error("Invalid String In Multiblock Structure Code!!!");
                        return false;
                }
            }
        }
        for (int y = yCoord - 1; y <= yCoord + 1; y++) {
            for (int x = xCoord - 1; x <= xCoord + 1; x++) {

                switch (block) {
                    case AIR:
                        if (!(x == xCoord && y == yCoord && zCoord - dist == zCoord)
                                && !isReplaceable(x, y, zCoord - dist, set))
                            return false;
                        break;
                    case REDSTONE:
                        if (!(x == xCoord && y == yCoord && zCoord - dist == zCoord)
                                && !testForOrActivateRedstone(x, y, zCoord - dist, set, activate))
                            return false;
                        break;
                    case DRACONIUM:
                        if (!(x == xCoord && y == yCoord && zCoord - dist == zCoord)
                                && !testForOrActivateDraconium(x, y, zCoord - dist, set, activate))
                            return false;
                        break;
                    default:
                        LogHelper.error("Invalid String In Multiblock Structure Code!!!");
                        return false;
                }
            }
        }

        return true;
    }

    private boolean testForOrActivateDraconium(int x, int y, int z, boolean set, boolean activate) {
        if (!activate) {
            if (set) {
                worldObj.setBlock(
                        x,
                        y,
                        z,
                        BalanceConfigHandler.energyStorageStructureOuterBlock,
                        BalanceConfigHandler.energyStorageStructureOuterBlockMetadata,
                        3);
                return true;
            } else {
                return isDraconiumBlock(x, y, z);
            }
        } else {
            return activateDraconium(x, y, z);
        }
    }

    private boolean isDraconiumBlock(int x, int y, int z) {
        Block block = worldObj.getBlock(x, y, z);
        int metadata = worldObj.getBlockMetadata(x, y, z);
        return (block == BalanceConfigHandler.energyStorageStructureOuterBlock
                && metadata == BalanceConfigHandler.energyStorageStructureOuterBlockMetadata)
                || (block == ModBlocks.invisibleMultiblock && metadata == 0);
    }

    private boolean activateDraconium(int x, int y, int z) {
        if (isDraconiumBlock(x, y, z)) {
            worldObj.setBlock(x, y, z, ModBlocks.invisibleMultiblock, 0, 2);
            TileEntity tile = worldObj.getTileEntity(x, y, z);
            if (tile instanceof TileInvisibleMultiblock) {
                ((TileInvisibleMultiblock) tile).master = new TileLocation(xCoord, yCoord, zCoord);
                return true;
            }
        }
        LogHelper.error("Failed to activate structure (activateDraconium)");
        return false;
    }

    private boolean testForOrActivateRedstone(int x, int y, int z, boolean set, boolean activate) {
        if (!activate) {
            if (set) {
                worldObj.setBlock(
                        x,
                        y,
                        z,
                        BalanceConfigHandler.energyStorageStructureBlock,
                        BalanceConfigHandler.energyStorageStructureBlockMetadata,
                        3);
                return true;
            } else {
                return isRedstoneBlock(x, y, z);
            }
        } else {
            return activateRedstone(x, y, z);
        }
    }

    private boolean isRedstoneBlock(int x, int y, int z) {
        Block block = worldObj.getBlock(x, y, z);
        int metadata = worldObj.getBlockMetadata(x, y, z);
        return (block == BalanceConfigHandler.energyStorageStructureBlock
                && metadata == BalanceConfigHandler.energyStorageStructureBlockMetadata)
                || (block == ModBlocks.invisibleMultiblock && metadata == 1);
    }

    private boolean activateRedstone(int x, int y, int z) {
        if (isRedstoneBlock(x, y, z)) {
            worldObj.setBlock(x, y, z, ModBlocks.invisibleMultiblock, 1, 2);
            TileEntity tile = worldObj.getTileEntity(x, y, z);
            if (tile instanceof TileInvisibleMultiblock) {
                ((TileInvisibleMultiblock) tile).master = new TileLocation(xCoord, yCoord, zCoord);
                return true;
            }
        }
        LogHelper.error("Failed to activate structure (activateRedstone)");
        return false;
    }

    private boolean isReplaceable(int x, int y, int z, boolean set) {
        if (set) {
            worldObj.setBlock(x, y, z, Blocks.air);
            return true;
        } else return worldObj.getBlock(x, y, z).isReplaceable(worldObj, x, y, z) || worldObj.isAirBlock(x, y, z);
    }

    public boolean isOnline() {
        return online;
    }

    private void activateStabilizers() {
        for (TileLocation stabilizer : stabilizers) {
            if (stabilizer == null) {
                LogHelper.error("activateStabilizers: detected null stabilizer!");
                return;
            }
            TileEntity tile = stabilizer.getTileEntity(worldObj);
            if (!(tile instanceof TileParticleGenerator)) {
                LogHelper.error("Missing Tile Entity (Particle Generator)");
                return;
            }
            TileParticleGenerator generator = (TileParticleGenerator) tile;
            generator.stabalizerMode = true;
            generator.setMaster(new TileLocation(xCoord, yCoord, zCoord));
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
        long capacity = 0;
        switch (tier) {
            case 0:
                capacity = BalanceConfigHandler.energyStorageTier1Storage;
                break;
            case 1:
                capacity = BalanceConfigHandler.energyStorageTier2Storage;
                break;
            case 2:
                capacity = BalanceConfigHandler.energyStorageTier3Storage;
                break;
            case 3:
                capacity = BalanceConfigHandler.energyStorageTier4Storage;
                break;
            case 4:
                capacity = BalanceConfigHandler.energyStorageTier5Storage;
                break;
            case 5:
                capacity = BalanceConfigHandler.energyStorageTier6Storage;
                break;
            case 6:
                capacity = BalanceConfigHandler.energyStorageTier7Storage;
                break;
        }
        this.capacity = capacity;
        if (energy > capacity) energy = capacity;
    }

    public void deactivateStabilizers() {
        for (TileLocation stabilizer : stabilizers) {
            if (stabilizer != null) {
                TileEntity tile = stabilizer.getTileEntity(worldObj);
                if (tile instanceof TileParticleGenerator) {
                    TileParticleGenerator generator = (TileParticleGenerator) tile;
                    generator.stabalizerMode = false;
                    worldObj.setBlockMetadataWithNotify(
                            stabilizer.getXCoord(),
                            stabilizer.getYCoord(),
                            stabilizer.getZCoord(),
                            0,
                            2);
                }
            } else {
                LogHelper.error("deactivateStabilizers: detected null stabilizer!");
            }
        }
    }

    private boolean areStabilizersActive() {
        for (TileLocation stabilizer : stabilizers) {
            if (stabilizer == null) {
                LogHelper.error("areStabilizersActive: detected null stabilizer!");
                return false;
            }
            TileEntity tile = stabilizer.getTileEntity(worldObj);
            if (!(tile instanceof TileParticleGenerator)) {
                return false;
            }
            TileParticleGenerator generator = (TileParticleGenerator) tile;
            if (!generator.stabalizerMode
                    || worldObj.getBlockMetadata(stabilizer.getXCoord(), stabilizer.getYCoord(), stabilizer.getZCoord())
                            != 1)
                return false;
        }
        return true;
    }

    private boolean checkStabilizers() {
        for (TileLocation stabilizer : stabilizers) {
            if (stabilizer == null) {
                return false;
            }
            TileEntity tile = stabilizer.getTileEntity(worldObj);
            if (!(tile instanceof TileParticleGenerator)) {
                return false;
            }
            TileParticleGenerator generator = (TileParticleGenerator) tile;
            if (!generator.stabalizerMode) {
                return false;
            }
            TileEnergyStorageCore core = generator.getMaster();
            if (core.xCoord != xCoord || core.yCoord != yCoord || core.zCoord != zCoord) {
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
            if (stabilizers[i] != null) stabilizers[i].writeToNBT(compound, String.valueOf(i));
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        online = compound.getBoolean("Online");
        tier = compound.getShort("Tier");
        energy = compound.getLong("EnergyL");
        if (compound.hasKey("Energy")) energy = (long) compound.getDouble("Energy");
        for (int i = 0; i < stabilizers.length; i++) {
            if (stabilizers[i] != null) stabilizers[i].readFromNBT(compound, String.valueOf(i));
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
                        + Utills.formatNumber(energy)
                        + " / "
                        + Utills.formatNumber(capacity)
                        + " ["
                        + Utills.addCommas(energy)
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

    private void detectAndRendChanges() {
        if (lastTickCapacity != energy) lastTickCapacity = (Long) sendObjectToClient(
                References.LONG_ID,
                0,
                energy,
                new NetworkRegistry.TargetPoint(worldObj.provider.dimensionId, xCoord, yCoord, zCoord, 20));
    }

    @Override
    public void receiveObjectFromServer(int index, Object object) {
        energy = (Long) object;
    }
}
