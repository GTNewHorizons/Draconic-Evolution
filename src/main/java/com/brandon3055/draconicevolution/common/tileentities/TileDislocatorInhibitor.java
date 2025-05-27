package com.brandon3055.draconicevolution.common.tileentities;

import java.util.HashMap;
import java.util.HashSet;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.brandon3055.draconicevolution.common.ModBlocks;
import com.brandon3055.draconicevolution.common.container.ContainerDislocatorInhibitor;

public class TileDislocatorInhibitor extends TileEntity implements IInventory {

    public static final int MAXIMUM_RANGE = 16;
    public static final int MINIMUM_RANGE = 1;
    public static final int FILTER_SLOTS = 8;
    public static final HashMap<World, HashSet<TileDislocatorInhibitor>> inhibitors = new HashMap<>();

    private boolean redstoneActive = false;
    private ActivityControlType activityControlType = ActivityControlType.ALWAYS_ACTIVE;
    private boolean registered = false;
    private int range = 5;

    private boolean whitelist = false;
    private final ItemStack[] filter = new ItemStack[FILTER_SLOTS];

    public static boolean isBlockedByInhibitor(World world, EntityItem item) {
        HashSet<TileDislocatorInhibitor> list = inhibitors.get(world);
        if (list == null) {
            return false;
        }
        for (TileDislocatorInhibitor inhibitor : list) {
            if (!inhibitor.shouldBeActive()) {
                continue;
            }
            if (!inhibitor.isBlockingItem(item.getEntityItem())) {
                continue;
            }
            if (inhibitor.isInRange(item.posX, item.posY, item.posZ)) {
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
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    public boolean isBlockingItem(ItemStack item) {
        boolean blocked = false;
        for (ItemStack itemStack : filter) {
            if (itemStack == null) {
                continue;
            }
            if (itemStack.isItemEqual(item)) {
                blocked = true;
                break;
            }
        }
        if (whitelist) {
            return blocked;
        }
        return !blocked;
    }

    public boolean shouldBeActive() {
        switch (activityControlType) {
            case NEVER_ACTIVE -> {
                return false;
            }
            case WITH_REDSTONE -> {
                return redstoneActive;
            }
            case WITHOUT_REDSTONE -> {
                return !redstoneActive;
            }
            default -> {
                return true;
            }
        }
    }

    public boolean isInRange(double x, double y, double z) {
        return x >= this.xCoord - range && x <= this.xCoord + range + 1
                && y >= this.yCoord - range
                && y <= this.yCoord + range + 1
                && z >= this.zCoord - range
                && z <= this.zCoord + range + 1;
    }

    public void setActivityControl(int index) {
        if (index < 0 || index >= ActivityControlType.values().length) {
            return;
        }
        this.activityControlType = ActivityControlType.values()[index];
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    public ActivityControlType getActivityControlType() {
        return activityControlType;
    }

    public boolean isRedstoneActive() {
        return redstoneActive;
    }

    public void setRedstoneActive(boolean redstoneActive) {
        this.redstoneActive = redstoneActive;
    }

    public void setFilterItem(int slot, ItemStack item) {
        if (slot < 0 || slot >= FILTER_SLOTS) {
            return;
        }
        setInventorySlotContents(slot, item);

        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    public void addItemFilter(ItemStack itemStack) {
        if (itemStack == null) {
            return;
        }
        for (int i = 0; i < FILTER_SLOTS; i++) {
            if (filter[i] == null) {
                setInventorySlotContents(i, itemStack);
                return;
            }
        }
    }

    public boolean isWhitelist() {
        return whitelist;
    }

    public void setWhitelist(boolean value) {
        this.whitelist = value;
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    @Override
    public String getInventoryName() {
        return StatCollector.translateToLocal(ModBlocks.dislocatorInhibitor.getUnlocalizedName() + ".name");
    }

    @Override
    public int getSizeInventory() {
        return FILTER_SLOTS;
    }

    @Override
    public ItemStack getStackInSlot(int slotIn) {
        if (slotIn < 0 || slotIn >= FILTER_SLOTS) {
            return null;
        }
        return this.filter[slotIn];
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        ItemStack item = this.filter[index];
        this.filter[index] = null;
        if (item == null) {
            return null;
        }
        item.stackSize = 0;
        return item;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int index) {
        return null;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        if (stack != null) {
            this.filter[index] = stack.copy();
            this.filter[index].stackSize = 0;
        } else {
            this.filter[index] = null;
        }
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return true;
    }

    @Override
    public void openInventory() {

    }

    @Override
    public void closeInventory() {

    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return true;
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    public Container getGuiContainer(InventoryPlayer playerInventory) {
        return new ContainerDislocatorInhibitor(playerInventory, this);
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
        compound.setBoolean("RedstoneAcitve", redstoneActive);
        compound.setString("ActivityControl", activityControlType.name());
        compound.setBoolean("Whitelist", whitelist);

        int i = 0;
        for (ItemStack item : filter) {
            NBTTagCompound itemTag = new NBTTagCompound();
            if (item != null) {
                item.writeToNBT(itemTag);
                compound.setTag("Item" + i, itemTag);
            }
            i++;
        }
        super.writeToNBT(compound);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        range = compound.getInteger("Range");
        redstoneActive = compound.getBoolean("RedstoneAcitve");
        activityControlType = ActivityControlType.valueOf(compound.getString("ActivityControl"));
        whitelist = compound.getBoolean("Whitelist");

        for (int i = 0; i < FILTER_SLOTS; i++) {
            NBTBase tag = compound.getTag("Item" + i);
            if (tag instanceof NBTTagCompound) {
                filter[i] = ItemStack.loadItemStackFromNBT((NBTTagCompound) tag);
            } else {
                filter[i] = null;
            }
        }
        super.readFromNBT(compound);
    }

    public enum ActivityControlType {

        ALWAYS_ACTIVE,
        WITH_REDSTONE,
        WITHOUT_REDSTONE,
        NEVER_ACTIVE;

        public ActivityControlType getNext() {
            return switch (this) {
                case ALWAYS_ACTIVE -> ActivityControlType.WITH_REDSTONE;
                case WITH_REDSTONE -> ActivityControlType.WITHOUT_REDSTONE;
                case WITHOUT_REDSTONE -> ActivityControlType.NEVER_ACTIVE;
                default -> ActivityControlType.ALWAYS_ACTIVE;
            };
        }

    }

}
