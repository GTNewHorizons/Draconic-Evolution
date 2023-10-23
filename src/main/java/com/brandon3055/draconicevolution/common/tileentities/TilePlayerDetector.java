package com.brandon3055.draconicevolution.common.tileentities;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

public class TilePlayerDetector extends TileEntity {

    public static final int MAXIMUM_RANGE = 10;
    public static final int MINIMUM_RANGE = 1;
    private static final int scanRate = 5;

    private int tick = 0;
    private boolean shouldOutput = false;
    private int range = 1;

    @Override
    public void updateEntity() {
        if (worldObj.isRemote) {
            return;
        }

        if (tick >= scanRate) {
            tick = 0;
            EntityPlayer player = worldObj.getClosestPlayer(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D, range + 0.5D);
            if (player != null) {
                if (!shouldOutput) setShouldOutput(true);
            } else {
                if (shouldOutput) setShouldOutput(false);
            }
        } else {
            tick++;
        }
    }

    public boolean shouldOutput() {
        return shouldOutput;
    }

    public void setShouldOutput(boolean shouldOutputSignal) {
        shouldOutput = shouldOutputSignal;
        updateBlocks();
    }

    public int getRange() {
        return range;
    }

    public void setRange(int value) {
        if (value > MAXIMUM_RANGE) {
            value = MINIMUM_RANGE;
        }
        if (value < MINIMUM_RANGE) {
            value = MAXIMUM_RANGE;
        }
        this.range = value;
        updateBlocks();
    }

    private void updateBlocks() {
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord, zCoord, getBlockType());
        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            worldObj.notifyBlocksOfNeighborChange(
                    xCoord + direction.offsetX,
                    yCoord + direction.offsetY,
                    zCoord + direction.offsetZ,
                    getBlockType(),
                    direction.getOpposite().ordinal());
        }
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound tagCompound = new NBTTagCompound();
        this.writeToNBT(tagCompound);
        return new S35PacketUpdateTileEntity(this.xCoord, this.yCoord, this.zCoord, 1, tagCompound);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        readFromNBT(pkt.func_148857_g());
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        compound.setBoolean("OutPut", shouldOutput);
        compound.setInteger("Range", range);
        super.writeToNBT(compound);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        shouldOutput = compound.getBoolean("OutPut");
        range = compound.getInteger("Range");
        super.readFromNBT(compound);
    }
}
