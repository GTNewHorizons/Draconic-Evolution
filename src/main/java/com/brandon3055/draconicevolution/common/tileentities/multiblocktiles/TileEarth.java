package com.brandon3055.draconicevolution.common.tileentities.multiblocktiles;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;

public class TileEarth extends TileEntity {

    // Prevent culling when block is out of frame so model can remain active.
    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }

    private static final int maxRotationSpeed = 64;
    private static final int maxSize = 32;

    private int size = 1;
    private int rotationSpeed = 0;

    public void incrementSize() {
        size++;
    }

    public void increaseRotationSpeed() {
        rotationSpeed++;
    }

    public int getSize() {
        return size % maxSize;
    }

    public int getRotationSpeed() {
        return rotationSpeed % maxRotationSpeed;
    }

    private static final String rotationSpeedNBTTag = "rotationSpeed";
    private static final String sizeNBTTag = "size";

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger(rotationSpeedNBTTag, rotationSpeed);
        compound.setInteger(sizeNBTTag, size);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        rotationSpeed = compound.getInteger(rotationSpeedNBTTag);
        size = compound.getInteger(sizeNBTTag);
    }
}
