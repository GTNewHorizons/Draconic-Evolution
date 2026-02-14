package com.brandon3055.draconicevolution.common.utils;

import net.minecraft.enchantment.Enchantment;

/**
 * Created by Brandon on 17/01/2015.
 */
public interface IInventoryTool extends IConfigurableItem {

    String getInventoryName();

    int getInventorySlots();

    boolean isEnchantValid(Enchantment enchant);
}
