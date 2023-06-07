package com.brandon3055.draconicevolution.common.container;

import java.util.Set;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import com.brandon3055.draconicevolution.common.ModBlocks;
import com.brandon3055.draconicevolution.common.ModItems;
import com.brandon3055.draconicevolution.common.inventory.GenericInventory;
import com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.reactor.TileReactorCore;
import com.brandon3055.draconicevolution.common.utills.OreDictionaryHelper;

/**
 * Created by brandon3055 on 30/7/2015.
 */
public class ContainerReactor extends ContainerDataSync {

    private static final int MaximumFuelStorage = 10368;
    private static final int NuggetFuelAmount = 16;
    private static final int IngotFuelAmount = NuggetFuelAmount * 9;
    private static final int BlockFuelAmount = IngotFuelAmount * 9;
    private final TileReactorCore core;
    private final EntityPlayer player;
    private final GenericInventory ioSlots = new GenericInventory() {

        private final ItemStack[] items = new ItemStack[2];

        @Override
        public ItemStack[] getStorage() {
            return items;
        }

        @Override
        public int getInventoryStackLimit() {
            return 1;
        }

        @Override
        public void setInventorySlotContents(int slot, ItemStack stack) {
            if (slot == 0) {
                Set<String> oreNames = OreDictionaryHelper.getOreNames(stack);
                if (oreNames.contains("blockDraconiumAwakened")) {
                    core.reactorFuel += stack.stackSize * BlockFuelAmount;
                } else if (oreNames.contains("ingotDraconiumAwakened")) {
                    core.reactorFuel += stack.stackSize * IngotFuelAmount;
                } else if (oreNames.contains("nuggetDraconiumAwakened")) {
                    core.reactorFuel += stack.stackSize * NuggetFuelAmount;
                }
            } else {
                items[slot] = stack;
            }
        }

        @Override
        public void markDirty() {
            super.markDirty();
            core.markDirty();
        }
    };

    private int conversionUnitCache = -1;
    private int tempDrainFactorCache = -1;
    private int generationRateCache = -1;
    private int fieldDrainCache = -1;
    private int fuelUseRateCache = -1;
    private boolean isOfflineCache;

    public ContainerReactor(EntityPlayer player, TileReactorCore core) {
        this.core = core;
        this.player = player;
        this.isOfflineCache = core.reactorState == TileReactorCore.STATE_OFFLINE;

        for (int x = 0; x < 9; x++) {
            addSlotToContainer(new Slot(player.inventory, x, 44 + 18 * x, 198));
        }

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                addSlotToContainer(new Slot(player.inventory, x + y * 9 + 9, 44 + 18 * x, 140 + y * 18));
            }
        }

        if (core.reactorState == TileReactorCore.STATE_OFFLINE) {
            addFuelSlots();
        }
    }

    private void addFuelSlots() {
        addSlotToContainer(new SlotInsert(ioSlots, 0, 15, 140, core));
        addSlotToContainer(new SlotExtract(ioSlots, 1, 217, 140));
    }

    @SuppressWarnings("unchecked")
    private void removeFuelSlots() {
        inventorySlots.removeIf(o -> o instanceof SlotExtract || o instanceof SlotInsert);
    }

    @Override
    public void onContainerClosed(EntityPlayer entityPlayer) {
        if (ioSlots.getStackInSlot(1) != null && !player.worldObj.isRemote) {
            entityPlayer.worldObj.spawnEntityInWorld(
                    new EntityItem(
                            player.worldObj,
                            entityPlayer.posX,
                            player.posY,
                            player.posZ,
                            ioSlots.getStackInSlot(1)));
            ioSlots.setInventorySlotContents(1, null);
        }
        super.onContainerClosed(entityPlayer);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) { // todo (the chest thing)
        return true;
    }

    @Override
    public void detectAndSendChanges() {
        if (isOfflineCache && core.reactorState != TileReactorCore.STATE_OFFLINE) {
            removeFuelSlots();
            sendObjectToClient(null, 99, 1);
        } else if (!isOfflineCache && core.reactorState == TileReactorCore.STATE_OFFLINE) {
            addFuelSlots();
            sendObjectToClient(null, 98, 1);
        }
        isOfflineCache = core.reactorState == TileReactorCore.STATE_OFFLINE;

        int conversionUnit = (int) (core.conversionUnit * 100);
        if (conversionUnit != conversionUnitCache) {
            conversionUnitCache = (int) sendObjectToClient(null, 0, conversionUnit);
        }
        int tempDrainFactor = (int) (core.tempDrainFactor * 1000);
        if (tempDrainFactor != tempDrainFactorCache) {
            tempDrainFactorCache = (int) sendObjectToClient(null, 8, tempDrainFactor);
        }
        int generationRate = (int) core.generationRate;
        if (generationRate != generationRateCache) {
            generationRateCache = (int) sendObjectToClient(null, 9, generationRate);
        }
        if (core.fieldDrain != fieldDrainCache) {
            fieldDrainCache = (int) sendObjectToClient(null, 10, core.fieldDrain);
        }
        int fuelUseRate = (int) (core.fuelUseRate * 1000000);
        if (fuelUseRate != fuelUseRateCache) {
            fuelUseRateCache = (int) sendObjectToClient(null, 11, fuelUseRate);
        }

        super.detectAndSendChanges();
    }

    @Override
    public void receiveSyncData(int index, int value) {
        if (index == 0) {
            core.conversionUnit = (double) value / 100D;
        } else if (index == 8) {
            core.tempDrainFactor = value / 1000D;
        } else if (index == 9) {
            core.generationRate = value;
        } else if (index == 10) {
            core.fieldDrain = value;
        } else if (index == 11) {
            core.fuelUseRate = value / 1000000D;
        }
        if (index == 20) {
            core.processButtonPress(value);
        }
        if (index == 99) {
            removeFuelSlots();
        } else if (index == 98) {
            addFuelSlots();
        }
    }

    @Override
    public ItemStack slotClick(int slot, int button, int mode, EntityPlayer player) {
        if (slot == 37 && player.inventory.getItemStack() == null) {
            if (core.reactorFuel / IngotFuelAmount >= 64) {
                int stackSize = core.reactorFuel / BlockFuelAmount;
                stackSize = Math.min(64, stackSize);
                ioSlots.setInventorySlotContents(1, new ItemStack(ModBlocks.draconicBlock, stackSize));
                core.reactorFuel -= stackSize * BlockFuelAmount;
            } else if (core.reactorFuel >= IngotFuelAmount) {
                int stackSize = core.reactorFuel / IngotFuelAmount;
                stackSize = Math.min(64, stackSize);
                ioSlots.setInventorySlotContents(1, new ItemStack(ModItems.draconicIngot, stackSize));
                core.reactorFuel -= stackSize * IngotFuelAmount;
            } else if (core.reactorFuel >= NuggetFuelAmount) {
                int stackSize = core.reactorFuel / NuggetFuelAmount;
                ioSlots.setInventorySlotContents(1, new ItemStack(ModItems.nugget, stackSize, 1));
                core.reactorFuel -= stackSize * NuggetFuelAmount;
            } else if (core.convertedFuel / IngotFuelAmount >= 64) {
                int stackSize = core.convertedFuel / BlockFuelAmount;
                stackSize = Math.min(64, stackSize);
                ioSlots.setInventorySlotContents(1, new ItemStack(ModItems.chaosFragment, stackSize, 2));
                core.convertedFuel -= stackSize * BlockFuelAmount;
            } else if (core.convertedFuel >= IngotFuelAmount) {
                int stackSize = core.convertedFuel / IngotFuelAmount;
                stackSize = Math.min(64, stackSize);
                ioSlots.setInventorySlotContents(1, new ItemStack(ModItems.chaosFragment, stackSize, 1));
                core.convertedFuel -= stackSize * IngotFuelAmount;
            } else if (core.convertedFuel >= NuggetFuelAmount) {
                int stackSize = core.convertedFuel / NuggetFuelAmount;
                ioSlots.setInventorySlotContents(1, new ItemStack(ModItems.chaosFragment, stackSize, 0));
                core.convertedFuel -= stackSize * NuggetFuelAmount;
            }
        }
        return super.slotClick(slot, button, mode, player);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        return null;
    }

    public static class SlotInsert extends Slot {

        private int stackSizeLimit = 0;
        private final TileReactorCore core;

        public SlotInsert(IInventory inventory, int slot, int x, int y, TileReactorCore core) {
            super(inventory, slot, x, y);
            this.core = core;
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            if (stack == null) {
                return false;
            }
            Set<String> oreNames = OreDictionaryHelper.getOreNames(stack);
            if (oreNames.contains("nuggetDraconiumAwakened")) {
                stackSizeLimit = (MaximumFuelStorage - (core.reactorFuel + core.convertedFuel)) / NuggetFuelAmount;
            } else if (oreNames.contains("ingotDraconiumAwakened")) {
                stackSizeLimit = (MaximumFuelStorage - (core.reactorFuel + core.convertedFuel)) / IngotFuelAmount;
            } else if (oreNames.contains("blockDraconiumAwakened")) {
                stackSizeLimit = (MaximumFuelStorage - (core.reactorFuel + core.convertedFuel)) / BlockFuelAmount;
            } else {
                return false;
            }
            return stackSizeLimit > 0;
        }

        @Override
        public int getSlotStackLimit() {
            return stackSizeLimit;
        }
    }

    public static class SlotExtract extends Slot {

        public SlotExtract(GenericInventory inventory, int slot, int x, int y) {
            super(inventory, slot, x, y);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return false;
        }
    }
}
