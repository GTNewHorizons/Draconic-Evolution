package com.brandon3055.draconicevolution.common.tileentities;

import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.util.ForgeDirection;

import com.brandon3055.draconicevolution.common.container.ContainerPlayerDetector;

public class TilePlayerDetectorAdvanced extends TileEntity implements IInventory {

    public static final int MAXIMUM_RANGE = 20;
    public static final int MINIMUM_RANGE = 1;
    private static final int scanRate = 5;

    public String[] names = new String[42];
    public boolean isInWhiteListMode = false;
    public boolean isOutputInverted = false;
    public int range = 10;
    private final ItemStack[] items;
    private int tick = 0;
    private boolean shouldOutput = false;
    private List<EntityLivingBase> entityList;

    public TilePlayerDetectorAdvanced() {
        for (int i = 0; i < names.length; i++) {
            if (names[i] == null) {
                names[i] = "";
            }
        }
        items = new ItemStack[1];
    }

    @Override
    public void updateEntity() {
        if (worldObj.isRemote) {
            return;
        }

        if (tick >= scanRate) {
            tick = 0;
            if (shouldEmit()) {
                if (!shouldOutput) setShouldOutput(true);
            } else {
                if (shouldOutput) setShouldOutput(false);
            }
        } else {
            tick++;
        }
    }

    private boolean shouldEmit() {
        findEntities();

        for (EntityLivingBase entity : entityList) {
            if (!(entity instanceof EntityPlayer)) {
                return false;
            }
            String name = entity.getCommandSenderName();
            return (isInWhiteListMode == isPlayerListed(name)) != isOutputInverted;
        }
        return isOutputInverted;
    }

    private void findEntities() {
        double startX = xCoord + 0.5 - range;
        double startY = yCoord + 0.5 - range;
        double startZ = zCoord + 0.5 - range;
        double endX = xCoord + 0.5 + range;
        double endY = yCoord + 0.5 + range;
        double endZ = zCoord + 0.5 + range;
        entityList = worldObj.getEntitiesWithinAABB(
                EntityPlayer.class,
                AxisAlignedBB.getBoundingBox(startX, startY, startZ, endX, endY, endZ));
    }

    public boolean shouldOutput() {
        return shouldOutput;
    }

    public void setShouldOutput(boolean shouldOutputSignal) {
        shouldOutput = shouldOutputSignal;
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

    private boolean isPlayerListed(String name) {
        if (name == null) {
            return false;
        }

        for (String listedName : names) {
            if (listedName.equals(name)) {
                return true;
            }
        }
        return false;
    }

    // ###################INVENTORY###########################
    public int getSizeInventory() {
        return items.length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return items[slot];
    }

    @Override
    public ItemStack decrStackSize(int slot, int count) {
        ItemStack stack = items[slot];
        if (stack != null) {
            if (stack.stackSize <= count) {
                setInventorySlotContents(slot, null);
            } else {
                stack = stack.splitStack(count);
                if (stack.stackSize == 0) {
                    setInventorySlotContents(slot, null);
                }
            }
        }
        return stack;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        ItemStack stack = getStackInSlot(slot);
        if (stack != null) {
            setInventorySlotContents(slot, null);
        }
        return stack;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        items[slot] = stack;
        if (stack != null && stack.stackSize > getInventoryStackLimit()) {
            stack.stackSize = getInventoryStackLimit();
        }
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    @Override
    public String getInventoryName() {
        return "";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        if (worldObj == null) {
            return true;
        }
        if (worldObj.getTileEntity(xCoord, yCoord, zCoord) != this) {
            return false;
        }
        return player.getDistanceSq(xCoord + 0.5, yCoord + 0.5, zCoord + 0.4) < 64;
    }

    @Override
    public void openInventory() {}

    @Override
    public void closeInventory() {}

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    // ######################################################

    public Packet getDescriptionPacket() {
        NBTTagCompound tagCompound = new NBTTagCompound();
        this.writeToNBT(tagCompound);
        return new S35PacketUpdateTileEntity(this.xCoord, this.yCoord, this.zCoord, 1, tagCompound);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        readFromNBT(pkt.func_148857_g());
    }

    public Container getGuiContainer(InventoryPlayer playerInventory) {
        return new ContainerPlayerDetector(playerInventory, this);
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        NBTTagCompound[] tag = new NBTTagCompound[items.length];

        for (int i = 0; i < items.length; i++) {
            tag[i] = new NBTTagCompound();

            if (items[i] != null) {
                tag[i] = items[i].writeToNBT(tag[i]);
            }

            compound.setTag("Item" + i, tag[i]);
        }

        for (int i = 0; i < names.length; i++) {
            String name = (names[i] != null) ? names[i] : "";
            compound.setString("Name_" + i, name);
        }

        compound.setBoolean("WhiteList", isInWhiteListMode);
        compound.setBoolean("Output", shouldOutput);
        compound.setInteger("Range", range);
        compound.setBoolean("OutputInverted", isOutputInverted);

        super.writeToNBT(compound);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        NBTTagCompound[] tag = new NBTTagCompound[items.length];

        for (int i = 0; i < items.length; i++) {
            tag[i] = compound.getCompoundTag("Item" + i);
            items[i] = ItemStack.loadItemStackFromNBT(tag[i]);
        }

        for (int i = 0; i < names.length; i++) names[i] = compound.getString("Name_" + i);

        isInWhiteListMode = compound.getBoolean("WhiteList");
        range = compound.getInteger("Range");
        shouldOutput = compound.getBoolean("Output");
        isOutputInverted = compound.getBoolean("OutputInverted");

        super.readFromNBT(compound);
    }
}
