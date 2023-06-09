package com.brandon3055.draconicevolution.common.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import com.brandon3055.draconicevolution.common.inventory.SlotOpaqueBlock;
import com.brandon3055.draconicevolution.common.tileentities.TilePlayerDetectorAdvanced;

public class ContainerPlayerDetector extends Container {

    private boolean shouldShowInventory = true;
    private final TilePlayerDetectorAdvanced detector;

    public ContainerPlayerDetector(InventoryPlayer playerInventory, TilePlayerDetectorAdvanced detector) {
        this.detector = detector;

        bindPlayerInventory(playerInventory);
        addContainerSlots(detector);
        updateContainerSlots();
    }

    private void bindPlayerInventory(InventoryPlayer invPlayer) {
        for (int x = 0; x < 9; x++) {
            addSlotToContainer(new Slot(invPlayer, x, 8 + 18 * x, 174));
        }

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                addSlotToContainer(new Slot(invPlayer, x + y * 9 + 9, 8 + 18 * x, 116 + y * 18));
            }
        }
    }

    public void addContainerSlots(TilePlayerDetectorAdvanced tileDetector) {
        addSlotToContainer(new SlotOpaqueBlock(tileDetector, 0, 145, 15));
    }

    public void updateContainerSlots() {
        for (Slot slot : (Iterable<Slot>) inventorySlots) {
            if (slot instanceof SlotOpaqueBlock) {
                if (shouldShowInventory) {
                    slot.xDisplayPosition = 143;
                    slot.yDisplayPosition = 20;
                } else {
                    slot.xDisplayPosition = -1000;
                    slot.yDisplayPosition = -1000;
                }
            } else {
                if (shouldShowInventory) {
                    if (slot.slotNumber < 9) {
                        slot.xDisplayPosition = 8 + 18 * slot.slotNumber;
                        slot.yDisplayPosition = 174;
                    } else if (slot.slotNumber < 36) {
                        slot.xDisplayPosition = 8 + 18 * (slot.slotNumber % 9);
                        slot.yDisplayPosition = 116 + (slot.slotNumber / 9 - 1) * 18;
                    }
                } else {
                    slot.xDisplayPosition = -1000;
                    slot.yDisplayPosition = -1000;
                }
            }
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return detector.isUseableByPlayer(player);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slot) {
        return null;
    }

    public boolean shouldShowInventory() {
        return shouldShowInventory;
    }

    public void setShouldShowInventory(boolean shouldShow) {
        shouldShowInventory = shouldShow;
        updateContainerSlots();
    }

    public TilePlayerDetectorAdvanced getDetector() {
        return detector;
    }
}
