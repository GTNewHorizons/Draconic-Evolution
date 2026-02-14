package com.brandon3055.draconicevolution.common.tileentities;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;

import com.brandon3055.draconicevolution.common.ModBlocks;
import com.brandon3055.draconicevolution.common.utils.LogHelper;

/**
 * Created by Brandon on 14/08/2014.
 */
public class TilePlacedItem extends TileEntity {

    public ItemStack stack;
    public float rotation = 0F;
    private boolean hasUpdated = false;

    @Override
    public void updateEntity() {
        if (!hasUpdated && stack != null) {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            hasUpdated = true;
        }
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound tagCompound = new NBTTagCompound();
        writeToNBT(tagCompound);
        return new S35PacketUpdateTileEntity(this.xCoord, this.yCoord, this.zCoord, 1, tagCompound);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        readFromNBT(pkt.func_148857_g());
    }

    public void setStack(ItemStack stack) {
        this.stack = stack;
        worldObj.scheduleBlockUpdate(xCoord, yCoord, zCoord, ModBlocks.placedItem, 20);
    }

    public ItemStack getStack() {
        return stack;
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        NBTTagCompound[] tag = new NBTTagCompound[1];
        tag[0] = new NBTTagCompound();
        if (stack != null) {
            stack.writeToNBT(tag[0]);
        }
        compound.setTag("Item" + 0, tag[0]);
        compound.setFloat("Rotation", rotation);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        NBTTagCompound[] tag = new NBTTagCompound[1];
        tag[0] = compound.getCompoundTag("Item" + 0);
        stack = ItemStack.loadItemStackFromNBT(tag[0]);
        if (stack == null) {
            // the stack can be null if the placed item was
            // an item that got removed in between mod updates
            stack = new ItemStack(Blocks.stone);
            this.invalidate();
            LogHelper.error(
                    "Cannot load the Placed Item at location "
                            + Vec3.createVectorHelper(this.xCoord, this.yCoord, this.zCoord)
                            + " because the associated item is null, it will be removed.");
        }
        rotation = compound.getFloat("Rotation");
    }
}
