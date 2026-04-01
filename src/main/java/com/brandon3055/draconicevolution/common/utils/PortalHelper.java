package com.brandon3055.draconicevolution.common.utils;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.brandon3055.draconicevolution.common.ModBlocks;
import com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.TileDislocatorReceptacle;
import com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.TilePortalBlock;

/**
 * Created by Brandon on 23/5/2015.
 */
public class PortalHelper {

    public static final int MAXIMUM_PORTAL_SIZE = 150;

    public static boolean isFrame(Block block) {
        return block == ModBlocks.infusedObsidian;
    }

    public static boolean isPortal(Block block) {
        return block == ModBlocks.portal;
    }

    public static PortalStructure getValidStructure(World world, int x, int y, int z) {
        if (world.isRemote) {
            return null;
        }

        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            for (ForgeDirection plane : ForgeDirection.VALID_DIRECTIONS) {
                if (plane != direction && plane != direction.getOpposite()) {
                    PortalStructure structure = traceFrame(world, x, y, z, direction, plane);
                    if (structure != null && structure.scanPortal(world, x, y, z, false, false)) {
                        return structure;
                    }
                }
            }
        }

        return null;
    }

    public static PortalStructure traceFrame(World world, int x, int y, int z, ForgeDirection startDir,
            ForgeDirection plane) {
        int startX = x + startDir.offsetX;
        int startY = y + startDir.offsetY;
        int startZ = z + startDir.offsetZ;

        if (!world.isAirBlock(startX, startY, startZ)) {
            return null;
        }

        int xSize = findDistanceToFrame(world, startX, startY, startZ, startDir);
        int ySize = findDistanceToFrame(world, startX, startY, startZ, plane);
        int yOffset = findDistanceToFrame(world, startX, startY, startZ, plane.getOpposite());
        ySize += yOffset - 1;

        if (xSize == 0 || ySize == 0 || yOffset == 0 || ySize > PortalHelper.MAXIMUM_PORTAL_SIZE) {
            return null;
        }

        PortalStructure structure = new PortalStructure(xSize, ySize, yOffset, startDir, plane);
        if (structure.checkFrameIsValid(world, x, y, z) && structure.scanPortal(world, x, y, z, false, false)) {
            return structure;
        }
        return null;

    }

    private static int findDistanceToFrame(World world, int startX, int startY, int startZ, ForgeDirection direction) {
        for (int distance = 1; distance <= PortalHelper.MAXIMUM_PORTAL_SIZE; distance++) {
            int targetX = startX + direction.offsetX * distance;
            int targetY = startY + direction.offsetY * distance;
            int targetZ = startZ + direction.offsetZ * distance;
            Block block = world.getBlock(targetX, targetY, targetZ);
            if (isFrame(block)) {
                return distance;
            }
            if (!world.isAirBlock(targetX, targetY, targetZ)) {
                return 0;
            }
        }
        return 0;
    }

    public static class PortalStructure {

        public int xSize;
        public int ySize;
        public int yOffset;
        public ForgeDirection startDir;
        public ForgeDirection plane;

        public PortalStructure() {}

        public PortalStructure(int xSize, int ySize, int yOffset, ForgeDirection startDir, ForgeDirection plane) {
            this.xSize = xSize;
            this.ySize = ySize;
            this.yOffset = yOffset;
            this.startDir = startDir;
            this.plane = plane;
        }

        public boolean checkFrameIsValid(World world, int x, int y, int z) {
            for (int xDistance = 1; xDistance <= xSize; xDistance++) {
                if (isFrameMissing(world, x, y, z, xDistance, -yOffset)
                        || isFrameMissing(world, x, y, z, xDistance, ySize - yOffset + 1)) {
                    return false;
                }
            }
            for (int yDistance = 1 - yOffset; yDistance <= ySize - yOffset; yDistance++) {
                if (yDistance != 0 && isFrameMissing(world, x, y, z, 0, yDistance)
                        || isFrameMissing(world, x, y, z, xSize + 1, yDistance)) {
                    return false;
                }
            }
            return true;
        }

        private boolean isFrameMissing(World world, int x, int y, int z, int xDistance, int yDistance) {
            int targetX = x + startDir.offsetX * xDistance + plane.offsetX * yDistance;
            int targetY = y + startDir.offsetY * xDistance + plane.offsetY * yDistance;
            int targetZ = z + startDir.offsetZ * xDistance + plane.offsetZ * yDistance;
            return !isFrame(world.getBlock(targetX, targetY, targetZ));
        }

        public boolean scanPortal(World world, int x, int y, int z, boolean setPortalBlocks,
                boolean checkPortalBlocks) {
            TileEntity tile = world.getTileEntity(x, y, z);
            if (!(tile instanceof TileDislocatorReceptacle receptacle)) {
                return false;
            }
            if (setPortalBlocks) {
                receptacle.updating = true;
            }

            for (int xDistance = 1; xDistance <= xSize; xDistance++) {
                for (int yDistance = 1 - yOffset; yDistance <= ySize - yOffset; yDistance++) {
                    int targetX = x + xDistance * startDir.offsetX + yDistance * plane.offsetX;
                    int targetY = y + xDistance * startDir.offsetY + yDistance * plane.offsetY;
                    int targetZ = z + xDistance * startDir.offsetZ + yDistance * plane.offsetZ;
                    Block block = world.getBlock(targetX, targetY, targetZ);

                    if (checkPortalBlocks) {
                        if (!isPortal(block)) {
                            return false;
                        }
                    } else if (setPortalBlocks) {
                        world.setBlock(targetX, targetY, targetZ, ModBlocks.portal);
                        tile = world.getTileEntity(targetX, targetY, targetZ);
                        if (tile instanceof TilePortalBlock portal) {
                            portal.masterX = x;
                            portal.masterY = y;
                            portal.masterZ = z;
                        }
                    } else if (!world.isAirBlock(targetX, targetY, targetZ)) {
                        return false;
                    }
                }
            }

            if (setPortalBlocks) {
                receptacle.updating = false;
            }

            return true;
        }

        public void writeToNBT(NBTTagCompound compound) {
            compound.setInteger("XSize", xSize);
            compound.setInteger("YSize", ySize);
            compound.setInteger("YOffset", yOffset);
            compound.setString("StartDir", startDir.name());
            compound.setString("Plane", plane.name());
        }

        public void readFromNBT(NBTTagCompound compound) {
            xSize = compound.getInteger("XSize");
            ySize = compound.getInteger("YSize");
            yOffset = compound.getInteger("YOffset");
            startDir = ForgeDirection.valueOf(compound.getString("StartDir"));
            plane = ForgeDirection.valueOf(compound.getString("Plane"));
        }
    }
}
