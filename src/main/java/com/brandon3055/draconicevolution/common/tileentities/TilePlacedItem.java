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

    /**
     * Pivot point on the mounting face and the two in-plane axes used to lay out the item grid, indexed by block
     * metadata (the side the display is attached to). Shared by the renderer and the hit test so that the stack a
     * player clicks on is the one they see.
     */
    public static final float[][] FACE_PIVOT = { { 0.5F, 1F, 0.5F }, { 0.5F, 0F, 0.5F }, { 0.5F, 0.5F, 1F },
            { 0.5F, 0.5F, 0F }, { 1F, 0.5F, 0.5F }, { 0F, 0.5F, 0.5F } };
    public static final float[][] FACE_AXIS_U = { { 1F, 0F, 0F }, { 1F, 0F, 0F }, { 1F, 0F, 0F }, { 1F, 0F, 0F },
            { 0F, 0F, 1F }, { 0F, 0F, 1F } };
    public static final float[][] FACE_AXIS_V = { { 0F, 0F, 1F }, { 0F, 0F, 1F }, { 0F, 1F, 0F }, { 0F, 1F, 0F },
            { 0F, 1F, 0F }, { 0F, 1F, 0F } };

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

    /**
     * Removes and returns the item at {@code index}, or null if the index is out of range.
     */
    public ItemStack removeItem(int index) {
        if (index < 0 || index >= stacks.size()) {
            return null;
        }
        ItemStack stack = stacks.remove(index);
        markDirtyAndSync();
        return stack;
    }

    /**
     * @return the number of columns (and maximum rows) in the grid used to display {@code count} items.
     */
    public static int getGridSize(int count) {
        return (int) Math.ceil(Math.sqrt(count));
    }

    /**
     * Computes where the centre of grid cell {@code index} sits relative to the face pivot, in block units along
     * {@link #FACE_AXIS_U} and {@link #FACE_AXIS_V}. Rows are centred so a partially filled last row does not hang to
     * one side.
     *
     * @return {u, v}
     */
    public static float[] getCellOffset(int index, int count) {
        int gridSize = getGridSize(count);
        if (gridSize <= 1) {
            return new float[] { 0F, 0F };
        }
        int rows = (count + gridSize - 1) / gridSize;
        float cell = 1F / gridSize;
        int row = index / gridSize;
        int col = index % gridSize;
        int rowLength = Math.min(gridSize, count - row * gridSize);
        return new float[] { (col - (rowLength - 1) / 2F) * cell, ((rows - 1) / 2F - row) * cell };
    }

    /**
     * Finds the stack whose grid cell is closest to a point on this block.
     *
     * @param meta the block metadata (mounting side)
     * @param hitX x of the hit, relative to the block (0..1)
     * @param hitY y of the hit, relative to the block (0..1)
     * @param hitZ z of the hit, relative to the block (0..1)
     * @return the index of the closest stack, or -1 if the display is empty
     */
    public int getStackIndexAt(int meta, float hitX, float hitY, float hitZ) {
        int count = stacks.size();
        if (count == 0) {
            return -1;
        }
        if (meta < 0 || meta > 5) {
            meta = 1;
        }
        float[] pivot = FACE_PIVOT[meta];
        float[] axisU = FACE_AXIS_U[meta];
        float[] axisV = FACE_AXIS_V[meta];
        float dx = hitX - pivot[0];
        float dy = hitY - pivot[1];
        float dz = hitZ - pivot[2];
        float u = dx * axisU[0] + dy * axisU[1] + dz * axisU[2];
        float v = dx * axisV[0] + dy * axisV[1] + dz * axisV[2];

        int closest = 0;
        float closestDistSq = Float.MAX_VALUE;
        for (int i = 0; i < count; i++) {
            float[] offset = getCellOffset(i, count);
            float du = u - offset[0];
            float dv = v - offset[1];
            float distSq = du * du + dv * dv;
            if (distSq < closestDistSq) {
                closestDistSq = distSq;
                closest = i;
            }
        }
        return closest;
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
