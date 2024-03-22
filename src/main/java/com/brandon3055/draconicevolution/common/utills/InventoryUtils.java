package com.brandon3055.draconicevolution.common.utills;

import java.util.Arrays;
import java.util.Optional;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import baubles.common.lib.PlayerHandler;

public final class InventoryUtils {

    private InventoryUtils() {}

    public static Optional<ItemStack> getItemInPlayerInventory(EntityPlayer player, Class<? extends Item> item) {

        Optional<ItemStack> blaublesSlots = Arrays.stream(PlayerHandler.getPlayerBaubles(player).stackList)
                .filter(itemStack -> itemStack != null && item.isInstance(itemStack.getItem())).findAny();
        if (blaublesSlots.isPresent()) {
            return blaublesSlots;
        }

        return Arrays.stream(player.inventory.mainInventory)
                .filter(itemStack -> itemStack != null && item.isInstance(itemStack.getItem())).findAny();
    }
}
