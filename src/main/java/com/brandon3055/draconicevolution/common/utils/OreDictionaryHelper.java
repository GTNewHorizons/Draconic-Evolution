package com.brandon3055.draconicevolution.common.utils;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

public class OreDictionaryHelper {

    public static Set<String> getOreNames(ItemStack stack) {
        int[] oreIds = OreDictionary.getOreIDs(stack);
        return Arrays.stream(oreIds).mapToObj(OreDictionary::getOreName).collect(Collectors.toSet());
    }
}
