package com.brandon3055.draconicevolution.common.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import com.brandon3055.draconicevolution.common.inventory.SlotFakeItem;
import com.brandon3055.draconicevolution.common.tileentities.TileDislocatorInhibitor;

public class ContainerDislocatorInhibitor extends Container {

    private TileDislocatorInhibitor inhibitor;

    public ContainerDislocatorInhibitor(InventoryPlayer playerInventory, TileDislocatorInhibitor inhibitor) {
        this.inhibitor = inhibitor;

        bindPlayerInventory(playerInventory);
        addContainerSlots(inhibitor);
    }

    private void bindPlayerInventory(InventoryPlayer invPlayer) {
        for (int x = 0; x < 9; x++) {
            addSlotToContainer(new Slot(invPlayer, x, 8 + 18 * x, 126));
        }

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                addSlotToContainer(new Slot(invPlayer, x + y * 9 + 9, 8 + 18 * x, 68 + y * 18));
            }
        }
    }

    public void addContainerSlots(TileDislocatorInhibitor tileInhibitor) {
        for (int i = 0; i < 8; i++) {
            addSlotToContainer(new SlotFakeItem(tileInhibitor, i, 8 + 18 * i, 42));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return inhibitor.isUseableByPlayer(player);
    }

    @Override
    public boolean canDragIntoSlot(Slot p_94531_1_) {
        return false;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slot) {
        this.inhibitor.addItemFilter(player.inventory.getStackInSlot(slot));
        return null;
    }

    public TileDislocatorInhibitor getTileEntity() {
        return inhibitor;
    }
}
