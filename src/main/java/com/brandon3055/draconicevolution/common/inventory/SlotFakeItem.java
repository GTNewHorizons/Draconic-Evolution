package com.brandon3055.draconicevolution.common.inventory;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class SlotFakeItem extends Slot {

    public SlotFakeItem(IInventory inventory, int id, int x, int y) {
        super(inventory, id, x, y);
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        // this is here so NEI CheatItemHandler does not eat the handleDragNDrop event
        return false;
    }

    @Override
    public int getSlotStackLimit() {
        return 1;
    }

    public boolean isOverSlot(int mouseX, int mouseY) {
        if (xDisplayPosition < mouseX && mouseX <= xDisplayPosition + 18) {
            return yDisplayPosition < mouseY && mouseY <= yDisplayPosition + 18;
        }
        return false;
    }

}
