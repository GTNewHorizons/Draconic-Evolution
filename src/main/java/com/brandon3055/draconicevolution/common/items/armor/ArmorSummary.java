package com.brandon3055.draconicevolution.common.items.armor;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import com.brandon3055.draconicevolution.common.utils.IUpgradableItem;
import com.brandon3055.draconicevolution.common.utils.ItemNBTHelper;

public final class ArmorSummary {

    private ArmorSummary() {}

    public static ArmorSummary get(EntityPlayer player) {
        // TODO if allocation is a problem replace with
        // a cached reusable thread local
        return new ArmorSummary().process(player);
    }

    /*---- Shield ----*/
    /**
     * Max protection points from all equipped armor peaces
     */
    public float maxProtectionPoints = 0F;
    /**
     * Total protection points from all equipped armor peaces
     */
    public float protectionPoints = 0F;
    /**
     * Number of quipped armor peaces
     */
    public int peaces = 0;
    /**
     * Point Allocation, The number of points on each peace
     */
    public float[] allocation;
    /**
     * How many points have been drained from each armor peace
     */
    public float[] pointsDown;
    /**
     * The armor peaces (Index will contain null if peace is not present)
     */
    public ItemStack[] armorStacks;
    /**
     * Mean Fatigue
     */
    public float entropy = 0F;
    /**
     * Mean Recovery Points
     */
    public int meanRecoveryPoints = 0;
    /**
     * Total RF stored in the armor
     */
    public long totalEnergyStored = 0;
    /**
     * Total Max RF storage for the armor
     */
    public long maxTotalEnergyStorage = 0;
    /**
     * RF stored in each armor peace
     */
    public int[] energyAllocation;
    /*---- Effects ----*/
    public boolean[] flight = new boolean[] { false, false, false };
    public float flightVModifier = 0F;
    public float speedModifier = 0F;
    public float jumpModifier = 0F;
    public float fireResistance = 0F;
    public float flightSpeedModifier = 0;
    public boolean hasHillStep = false;
    public boolean hasDraconic = false;

    private ArmorSummary process(EntityPlayer player) {
        final ItemStack[] armorSlots = player.inventory.armorInventory;
        float totalEntropy = 0;
        int totalRecoveryPoints = 0;
        boolean needInit = true;

        for (int i = 0; i < armorSlots.length; i++) {
            final ItemStack stack = armorSlots[i];
            if (stack == null || !(stack.getItem() instanceof ICustomArmor armor)) continue;
            if (needInit) {
                needInit = false;
                allocation = new float[armorSlots.length];
                armorStacks = new ItemStack[armorSlots.length];
                pointsDown = new float[armorSlots.length];
                energyAllocation = new int[armorSlots.length];
            }
            peaces++;
            allocation[i] = ItemNBTHelper.getFloat(stack, "ProtectionPoints", 0);
            protectionPoints += allocation[i];
            totalEntropy += ItemNBTHelper.getFloat(stack, "ShieldEntropy", 0);
            armorStacks[i] = stack;
            totalRecoveryPoints += IUpgradableItem.EnumUpgrade.SHIELD_RECOVERY.getUpgradePoints(stack);
            final float maxPoints = armor.getProtectionPoints(stack);
            pointsDown[i] = maxPoints - allocation[i];
            maxProtectionPoints += maxPoints;
            energyAllocation[i] = armor.getEnergyStored(stack);
            totalEnergyStored += energyAllocation[i];
            maxTotalEnergyStorage += armor.getMaxEnergyStored(stack);
            if (stack.getItem() instanceof DraconicArmor) hasDraconic = true;

            fireResistance += armor.getFireResistance(stack);

            switch (i) {
                case 2:
                    flight = armor.hasFlight(stack);
                    if (flight[0]) {
                        flightVModifier = armor.getFlightVModifier(stack, player);
                        flightSpeedModifier = armor.getFlightSpeedModifier(stack, player);
                    }
                    break;
                case 1:
                    speedModifier = armor.getSpeedModifier(stack, player);
                    break;
                case 0:
                    hasHillStep = armor.hasHillStep(stack, player);
                    jumpModifier = armor.getJumpModifier(stack, player);
                    break;
            }
        }

        if (peaces == 0) return null;

        entropy = totalEntropy / peaces;
        meanRecoveryPoints = totalRecoveryPoints / peaces;

        return this;
    }
}
