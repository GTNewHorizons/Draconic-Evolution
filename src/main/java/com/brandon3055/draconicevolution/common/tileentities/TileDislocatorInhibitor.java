package com.brandon3055.draconicevolution.common.tileentities;

import java.util.HashMap;
import java.util.HashSet;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class TileDislocatorInhibitor extends TileEntity {

    public static final int MAXIMUM_RANGE = 16;
    public static final int MINIMUM_RANGE = 1;
    public static final HashMap<World, HashSet<TileDislocatorInhibitor>> inhibitors = new HashMap<>();

    private boolean registered = false;
    private int range = 5;

    public static boolean isInInhibitorRange(World world, double x, double y, double z) {
        HashSet<TileDislocatorInhibitor> list = inhibitors.get(world);
        if (list == null) {
            return false;
        }
        for (TileDislocatorInhibitor inhibitor : list) {
            if (inhibitor.isInRange(x, y, z)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void updateEntity() {
        if (!registered) {
            HashSet<TileDislocatorInhibitor> map = inhibitors.computeIfAbsent(this.worldObj, k -> new HashSet<>());
            map.add(this);
            registered = true;
        }
    }

    public void unregister() {
        if (inhibitors.containsKey(this.worldObj)) {
            HashSet<TileDislocatorInhibitor> list = inhibitors.get(this.worldObj);
            list.remove(this);
            if (list.isEmpty()) {
                inhibitors.remove(this.worldObj);
            }
        }
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
    }

    public void increaseRange() {
        if (range < MAXIMUM_RANGE) {
            range++;
        }
    }

    public void decreaseRange() {
        if (range > MINIMUM_RANGE) {
            range--;
        }
    }

    public boolean isInRange(double x, double y, double z) {
        return x >= this.xCoord - range && x <= this.xCoord + range + 1
                && y >= this.yCoord - range
                && y <= this.yCoord + range + 1
                && z >= this.zCoord - range
                && z <= this.zCoord + range + 1;
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
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        compound.setInteger("Range", range);
        super.writeToNBT(compound);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        range = compound.getInteger("Range");
        super.readFromNBT(compound);
    }
}
