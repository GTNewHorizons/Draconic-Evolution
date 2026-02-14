package com.brandon3055.draconicevolution.common.utils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.InventoryLargeChest;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import baubles.common.lib.PlayerHandler;
import cpw.mods.fml.common.Loader;

public final class InventoryUtils {

    private static final boolean BAUBLES_MOD_IS_LOADED = Loader.isModLoaded("Baubles");

    private InventoryUtils() {}

    public static Optional<ItemStack> getItemInAnyPlayerInventory(EntityPlayer player,
            Class<? extends Item> itemClass) {
        if (BAUBLES_MOD_IS_LOADED) {
            Optional<ItemStack> itemInBaubles = getItemInPlayerBaublesInventory(player, itemClass);
            if (itemInBaubles.isPresent()) {
                return itemInBaubles;
            }
        }
        return getItemInPlayerInventory(player, itemClass);
    }

    public static Optional<ItemStack> getItemInPlayerInventory(EntityPlayer player, Class<? extends Item> itemClass) {
        if (itemClass != null) {
            for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                ItemStack itemStack = player.inventory.getStackInSlot(i);

                if (itemStack != null && itemClass.isInstance(itemStack.getItem())) {
                    return Optional.of(itemStack);
                }
            }
        }
        return Optional.empty();
    }

    public static Optional<ItemStack> getItemInPlayerBaublesInventory(EntityPlayer player,
            Class<? extends Item> itemClass) {
        if (BAUBLES_MOD_IS_LOADED && itemClass != null) {
            for (int i = 0; i < PlayerHandler.getPlayerBaubles(player).getSizeInventory(); i++) {
                ItemStack itemStack = PlayerHandler.getPlayerBaubles(player).getStackInSlot(i);
                if (itemStack != null && itemClass.isInstance(itemStack.getItem())) {
                    return Optional.of(itemStack);
                }
            }
        }
        return Optional.empty();
    }

    /***
     * Try to merge the supplied stack into the supplied slot in the target inventory
     *
     * @param targetInventory Although it doesn't return anything, it'll REDUCE the stack size of the stack that you
     *                        pass in
     *
     * @param slot
     * @param stack
     */
    public static void tryInsertStack(IInventory targetInventory, int slot, ItemStack stack, boolean canMerge) {
        if (targetInventory.isItemValidForSlot(slot, stack)) {
            ItemStack targetStack = targetInventory.getStackInSlot(slot);
            if (targetStack == null) {
                targetInventory.setInventorySlotContents(slot, stack.copy());
                stack.stackSize = 0;
            } else if (canMerge) {
                if (targetInventory.isItemValidForSlot(slot, stack) && areMergeCandidates(stack, targetStack)) {
                    int space = targetStack.getMaxStackSize() - targetStack.stackSize;
                    int mergeAmount = Math.min(space, stack.stackSize);
                    ItemStack copy = targetStack.copy();
                    copy.stackSize += mergeAmount;
                    targetInventory.setInventorySlotContents(slot, copy);
                    stack.stackSize -= mergeAmount;
                }
            }
        }
    }

    public static boolean areItemAndTagEqual(final ItemStack stackA, ItemStack stackB) {
        return stackA.isItemEqual(stackB) && ItemStack.areItemStackTagsEqual(stackA, stackB);
    }

    public static boolean areMergeCandidates(ItemStack source, ItemStack target) {
        return areItemAndTagEqual(source, target) && target.stackSize < target.getMaxStackSize();
    }

    public static void insertItemIntoInventory(IInventory inventory, ItemStack stack) {
        insertItemIntoInventory(inventory, stack, ForgeDirection.UNKNOWN, -1);
    }

    public static void insertItemIntoInventory(IInventory inventory, ItemStack stack, ForgeDirection side,
            int intoSlot) {
        insertItemIntoInventory(inventory, stack, side, intoSlot, true);
    }

    public static void insertItemIntoInventory(IInventory inventory, ItemStack stack, ForgeDirection side, int intoSlot,
            boolean doMove) {
        insertItemIntoInventory(inventory, stack, side, intoSlot, doMove, true);
    }

    public static void insertItemIntoInventory(IInventory inventory, ItemStack stack, ForgeDirection side, int intoSlot,
            boolean doMove, boolean canStack) {
        if (stack == null) return;

        final int sideId = side.ordinal();
        IInventory targetInventory = inventory;

        // if we're not meant to move, make a clone of the inventory
        if (!doMove) {
            GenericInventory copy = new GenericInventory(
                    "temporary.inventory",
                    false,
                    targetInventory.getSizeInventory());
            copy.copyFrom(inventory);
            targetInventory = copy;
        }

        final Set<Integer> attemptSlots = Sets.newTreeSet();

        // if it's a sided inventory, get all the accessible slots
        final boolean isSidedInventory = inventory instanceof ISidedInventory && side != ForgeDirection.UNKNOWN;

        if (isSidedInventory) {
            int[] accessibleSlots = ((ISidedInventory) inventory).getAccessibleSlotsFromSide(sideId);
            for (int slot : accessibleSlots) attemptSlots.add(slot);
        } else {
            // if it's just a standard inventory, get all slots
            for (int a = 0; a < inventory.getSizeInventory(); a++) {
                attemptSlots.add(a);
            }
        }

        // if we've defining a specific slot, we'll just use that
        if (intoSlot > -1) attemptSlots.retainAll(ImmutableSet.of(intoSlot));

        if (attemptSlots.isEmpty()) return;

        for (Integer slot : attemptSlots) {
            if (stack.stackSize <= 0) break;
            if (isSidedInventory && !((ISidedInventory) inventory).canInsertItem(slot, stack, sideId)) continue;
            tryInsertStack(targetInventory, slot, stack, canStack);
        }
    }

    public static int moveItemInto(IInventory fromInventory, int fromSlot, Object target, int intoSlot, int maxAmount,
            ForgeDirection direction, boolean doMove) {
        return moveItemInto(fromInventory, fromSlot, target, intoSlot, maxAmount, direction, doMove, true);
    }

    /***
     * Move an item from the fromInventory, into the target. The target can be an inventory or pipe. Double checks are
     * automagically wrapped. If you're not bothered what slot you insert into, pass -1 for intoSlot. If you're passing
     * false for doMove, it'll create a dummy inventory and its calculations on that instead
     *
     * @param fromInventory the inventory the item is coming from
     * @param fromSlot      the slot the item is coming from
     * @param target        the inventory you want the item to be put into. can be BC pipe or IInventory
     * @param intoSlot      the target slot. Pass -1 for any slot
     * @param maxAmount     The maximum amount you wish to pass
     * @param direction     The direction of the move. Pass UNKNOWN if not applicable
     * @param doMove
     * @param canStack
     * @return The amount of items moved
     */
    public static int moveItemInto(IInventory fromInventory, int fromSlot, Object target, int intoSlot, int maxAmount,
            ForgeDirection direction, boolean doMove, boolean canStack) {

        fromInventory = getInventory(fromInventory);

        // if we dont have a stack in the source location, return 0
        ItemStack sourceStack = fromInventory.getStackInSlot(fromSlot);
        if (sourceStack == null) {
            return 0;
        }

        if (fromInventory instanceof ISidedInventory
                && !((ISidedInventory) fromInventory).canExtractItem(fromSlot, sourceStack, direction.ordinal()))
            return 0;

        // create a clone of our source stack and set the size to either
        // maxAmount or the stackSize
        ItemStack clonedSourceStack = sourceStack.copy();
        clonedSourceStack.stackSize = Math.min(clonedSourceStack.stackSize, maxAmount);
        int amountToMove = clonedSourceStack.stackSize;
        int inserted = 0;

        if (target instanceof IInventory) {
            IInventory targetInventory = getInventory((IInventory) target);
            ForgeDirection side = direction.getOpposite();
            // try insert the item into the target inventory. this'll reduce the
            // stackSize of our stack
            insertItemIntoInventory(targetInventory, clonedSourceStack, side, intoSlot, doMove, canStack);
            inserted = amountToMove - clonedSourceStack.stackSize;
        }

        // if we've done the move, reduce/remove the stack from our source
        // inventory
        if (doMove) {
            ItemStack newSourcestack = sourceStack.copy();
            newSourcestack.stackSize -= inserted;
            if (newSourcestack.stackSize == 0) {
                fromInventory.setInventorySlotContents(fromSlot, null);
            } else {
                fromInventory.setInventorySlotContents(fromSlot, newSourcestack);
            }
        }

        return inserted;
    }

    private static IInventory doubleChestFix(TileEntity te) {
        final World world = te.getWorldObj();
        final int x = te.xCoord;
        final int y = te.yCoord;
        final int z = te.zCoord;
        if (world.getBlock(x - 1, y, z) == Blocks.chest) return new InventoryLargeChest(
                "Large chest",
                (IInventory) world.getTileEntity(x - 1, y, z),
                (IInventory) te);
        if (world.getBlock(x + 1, y, z) == Blocks.chest) return new InventoryLargeChest(
                "Large chest",
                (IInventory) te,
                (IInventory) world.getTileEntity(x + 1, y, z));
        if (world.getBlock(x, y, z - 1) == Blocks.chest) return new InventoryLargeChest(
                "Large chest",
                (IInventory) world.getTileEntity(x, y, z - 1),
                (IInventory) te);
        if (world.getBlock(x, y, z + 1) == Blocks.chest) return new InventoryLargeChest(
                "Large chest",
                (IInventory) te,
                (IInventory) world.getTileEntity(x, y, z + 1));
        return (te instanceof IInventory) ? (IInventory) te : null;
    }

    public static IInventory getInventory(IInventory inventory) {
        if (inventory instanceof TileEntityChest) return doubleChestFix((TileEntity) inventory);
        return inventory;
    }

    public static List<ItemStack> getInventoryContents(IInventory inventory) {
        List<ItemStack> result = Lists.newArrayList();
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack slot = inventory.getStackInSlot(i);
            if (slot != null) result.add(slot);
        }
        return result;
    }

    public static void tryMergeStacks(ItemStack stackToMerge, ItemStack stackInSlot) {
        if (stackInSlot == null || !stackInSlot.isItemEqual(stackToMerge)
                || !ItemStack.areItemStackTagsEqual(stackToMerge, stackInSlot))
            return;

        int newStackSize = stackInSlot.stackSize + stackToMerge.stackSize;

        final int maxStackSize = stackToMerge.getMaxStackSize();
        if (newStackSize <= maxStackSize) {
            stackToMerge.stackSize = 0;
            stackInSlot.stackSize = newStackSize;
        } else if (stackInSlot.stackSize < maxStackSize) {
            stackToMerge.stackSize -= maxStackSize - stackInSlot.stackSize;
            stackInSlot.stackSize = maxStackSize;
        }

    }

    public static class GenericInventory implements IInventory {

        protected String inventoryTitle;
        protected int slotsCount;
        protected ItemStack[] inventoryContents;
        protected boolean isInvNameLocalized;

        public GenericInventory(String name, boolean isInvNameLocalized, int size) {
            this.isInvNameLocalized = isInvNameLocalized;
            this.slotsCount = size;
            this.inventoryTitle = name;
            this.inventoryContents = new ItemStack[size];
        }

        @Override
        public ItemStack decrStackSize(int par1, int par2) {
            if (this.inventoryContents[par1] != null) {
                ItemStack itemstack;

                if (this.inventoryContents[par1].stackSize <= par2) {
                    itemstack = this.inventoryContents[par1];
                    this.inventoryContents[par1] = null;
                    return itemstack;
                }
                itemstack = this.inventoryContents[par1].splitStack(par2);
                if (this.inventoryContents[par1].stackSize == 0) {
                    this.inventoryContents[par1] = null;
                }

                return itemstack;
            }
            return null;
        }

        @Override
        public int getInventoryStackLimit() {
            return 64;
        }

        @Override
        public int getSizeInventory() {
            return slotsCount;
        }

        @Override
        public ItemStack getStackInSlot(int i) {
            return this.inventoryContents[i];
        }

        public ItemStack getStackInSlot(Enum<?> i) {
            return getStackInSlot(i.ordinal());
        }

        @Override
        public ItemStack getStackInSlotOnClosing(int i) {
            if (i >= this.inventoryContents.length) {
                return null;
            }
            if (this.inventoryContents[i] != null) {
                ItemStack itemstack = this.inventoryContents[i];
                this.inventoryContents[i] = null;
                return itemstack;
            }
            return null;
        }

        @Override
        public boolean isItemValidForSlot(int i, ItemStack itemstack) {
            return true;
        }

        @Override
        public boolean isUseableByPlayer(EntityPlayer entityplayer) {
            return true;
        }

        public void readFromNBT(NBTTagCompound tag) {
            if (tag.hasKey("size")) {
                this.slotsCount = tag.getInteger("size");
            }
            NBTTagList nbttaglist = tag.getTagList("Items", 10);
            inventoryContents = new ItemStack[slotsCount];
            for (int i = 0; i < nbttaglist.tagCount(); i++) {
                NBTTagCompound stacktag = nbttaglist.getCompoundTagAt(i);
                int j = stacktag.getByte("Slot");
                if (j >= 0 && j < inventoryContents.length) {
                    inventoryContents[j] = ItemStack.loadItemStackFromNBT(stacktag);
                }
            }
        }

        @Override
        public void setInventorySlotContents(int i, ItemStack itemstack) {
            this.inventoryContents[i] = itemstack;

            if (itemstack != null && itemstack.stackSize > getInventoryStackLimit()) {
                itemstack.stackSize = getInventoryStackLimit();
            }
        }

        public void writeToNBT(NBTTagCompound tag) {
            tag.setInteger("size", getSizeInventory());
            NBTTagList nbttaglist = new NBTTagList();
            for (int i = 0; i < inventoryContents.length; i++) {
                if (inventoryContents[i] != null) {
                    NBTTagCompound stacktag = new NBTTagCompound();
                    stacktag.setByte("Slot", (byte) i);
                    inventoryContents[i].writeToNBT(stacktag);
                    nbttaglist.appendTag(stacktag);
                }
            }
            tag.setTag("Items", nbttaglist);
        }

        /**
         * This bastard never even gets called, so don't rely on it.
         */
        @Override
        public void markDirty() {}

        public void copyFrom(IInventory inventory) {
            for (int i = 0; i < inventory.getSizeInventory(); i++) {
                if (i < getSizeInventory()) {
                    ItemStack stack = inventory.getStackInSlot(i);
                    if (stack != null) {
                        setInventorySlotContents(i, stack.copy());
                    } else {
                        setInventorySlotContents(i, null);
                    }
                }
            }
        }

        @Override
        public String getInventoryName() {
            return this.inventoryTitle;
        }

        @Override
        public boolean hasCustomInventoryName() {
            return this.isInvNameLocalized;
        }

        @Override
        public void openInventory() {}

        @Override
        public void closeInventory() {}
    }
}
