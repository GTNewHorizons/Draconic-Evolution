package com.brandon3055.draconicevolution.common.tileentities;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;

import com.brandon3055.draconicevolution.common.ModBlocks;
import com.brandon3055.draconicevolution.common.handler.ConfigHandler;
import com.brandon3055.draconicevolution.common.utils.LogHelper;

/**
 * Created by Brandon on 14/08/2014.
 */
public class TilePlacedItem extends TileEntity {

    public static final int ABSOLUTE_MAX_STACKS = 16;

    private final List<ItemStack> stacks = new ArrayList<>(ABSOLUTE_MAX_STACKS);
    public float rotation = 0F;
    private boolean hasUpdated = false;
    private AxisAlignedBB renderBoundingBox;

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        if (renderBoundingBox == null) {
            renderBoundingBox = AxisAlignedBB
                    .getBoundingBox(xCoord - 2, yCoord - 2, zCoord - 2, xCoord + 2, yCoord + 2, zCoord + 2);
        }
        return renderBoundingBox;
    }

    @Override
    public void updateEntity() {
        if (!hasUpdated && !stacks.isEmpty()) {
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
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    public void setStack(ItemStack stack) {
        stacks.clear();
        if (stack != null) {
            stacks.add(stack);
        }
        worldObj.scheduleBlockUpdate(xCoord, yCoord, zCoord, ModBlocks.placedItem, 20);
    }

    /**
     * Adds an item to this display if there is room for it. The limit is the lower of the configured stack limit and
     * {@link #ABSOLUTE_MAX_STACKS}.
     *
     * @return true if the item was added.
     */
    public boolean addItem(ItemStack stack) {
        if (stack == null || stacks.size() >= getStackLimit()) {
            return false;
        }
        stacks.add(stack);
        markDirtyAndSync();
        return true;
    }

    /**
     * Removes and returns the most recently added item, or null if empty.
     */
    public ItemStack removeLastItem() {
        if (stacks.isEmpty()) {
            return null;
        }
        ItemStack stack = stacks.remove(stacks.size() - 1);
        markDirtyAndSync();
        return stack;
    }

    public static int getStackLimit() {
        return Math.min(ConfigHandler.placedItemStackLimit, ABSOLUTE_MAX_STACKS);
    }

    private void markDirtyAndSync() {
        markDirty();
        if (worldObj != null) {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    /**
     * @return the first stack in this display, or null if empty. Retained for the single-item code paths (pick block,
     *         collision box).
     */
    public ItemStack getStack() {
        return stacks.isEmpty() ? null : stacks.get(0);
    }

    public ItemStack getStack(int index) {
        return index >= 0 && index < stacks.size() ? stacks.get(index) : null;
    }

    public List<ItemStack> getStacks() {
        return stacks;
    }

    public int getDisplayCount() {
        return stacks.size();
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setByte("Count", (byte) stacks.size());
        for (int i = 0; i < stacks.size(); i++) {
            NBTTagCompound tag = new NBTTagCompound();
            stacks.get(i).writeToNBT(tag);
            compound.setTag("Item" + i, tag);
        }
        compound.setFloat("Rotation", rotation);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        stacks.clear();
        // Old worlds have no Count tag and always stored a single stack in Item0.
        int count = compound.hasKey("Count") ? compound.getByte("Count") : 1;
        for (int i = 0; i < count && i < ABSOLUTE_MAX_STACKS; i++) {
            ItemStack stack = ItemStack.loadItemStackFromNBT(compound.getCompoundTag("Item" + i));
            if (stack != null) {
                stacks.add(stack);
            } else {
                // the stack can be null if the placed item was
                // an item that got removed in between mod updates
                LogHelper.error(
                        "Cannot load a Placed Item stack at location "
                                + Vec3.createVectorHelper(this.xCoord, this.yCoord, this.zCoord)
                                + " because the associated item is null, it will be removed.");
            }
        }
        if (stacks.isEmpty()) {
            this.invalidate();
        }
        rotation = compound.getFloat("Rotation");
    }
}
