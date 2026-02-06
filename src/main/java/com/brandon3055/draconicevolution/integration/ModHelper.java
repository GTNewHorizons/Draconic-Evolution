package com.brandon3055.draconicevolution.integration;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

import com.brandon3055.draconicevolution.common.items.armor.CustomArmorHandler.ArmorSummery;
import com.brandon3055.draconicevolution.common.utils.LogHelper;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.hazards.Hazard;
import gregtech.api.items.MetaGeneratedTool;

/**
 * Created by brandon3055 on 29/9/2015.
 */
public class ModHelper {

    private static final boolean isTConInstalled;
    private static final boolean isAvaritiaInstalled;
    private static final boolean isGregTechInstalled;
    private static final boolean isAE2Installed;

    public static boolean isGTNHLibLoaded;
    public static boolean isHodgepodgeLoaded;

    private static Item cleaver;
    private static Item avaritiaSword;

    private static Class<?> AE2FItem;

    static {
        isTConInstalled = Loader.isModLoaded("TConstruct");
        isAvaritiaInstalled = Loader.isModLoaded("Avaritia");
        isGregTechInstalled = Loader.isModLoaded("gregtech_nh");
        isAE2Installed = Loader.isModLoaded("appliedenergistics2");
        isGTNHLibLoaded = Loader.isModLoaded("gtnhlib");
        isHodgepodgeLoaded = Loader.isModLoaded("hodgepodge");

        final String AE2_FITEM_CLASS = "appeng.entity.EntityFloatingItem";

        if (isAE2Installed) try {
            AE2FItem = Class.forName(AE2_FITEM_CLASS);
        } catch (ClassNotFoundException e) {
            LogHelper.error("Couldn't reflect class " + AE2_FITEM_CLASS);
        }
    }

    public static boolean isHoldingCleaver(EntityPlayer player) {
        if (!isTConInstalled) return false;
        else if (cleaver == null) cleaver = GameRegistry.findItem("TConstruct", "cleaver");

        return cleaver != null && player.getHeldItem() != null && player.getHeldItem().getItem().equals(cleaver);
    }

    public static boolean isHoldingAvaritiaSword(EntityPlayer player) {
        if (!isAvaritiaInstalled) return false;
        else if (avaritiaSword == null) avaritiaSword = GameRegistry.findItem("Avaritia", "Infinity_Sword");

        return avaritiaSword != null && player.getHeldItem() != null
                && player.getHeldItem().getItem().equals(avaritiaSword);
    }

    public static float applyModDamageAdjustments(ArmorSummery summery, LivingAttackEvent event) {
        if (summery == null) return event.ammount;
        EntityPlayer attacker = event.source.getEntity() instanceof EntityPlayer
                ? (EntityPlayer) event.source.getEntity()
                : null;

        if (attacker == null) {
            return event.ammount;
        }

        if (isHoldingAvaritiaSword(attacker)) {
            event.entityLiving.hurtResistantTime = 0;
            return 300F;
        } else if (event.source.isUnblockable() || event.source.canHarmInCreative()) {
            summery.entropy += 3;

            if (summery.entropy > 100) {
                summery.entropy = 100;
            }

            return event.ammount * 2;
        }

        return event.ammount;
    }

    public static boolean isGregTechEnchantmentItem(ItemStack stack) {
        if (stack == null) return false;
        if (stack.getItem() == null) return false;
        if (!isGregTechInstalled) return false;

        return stack.getItem() instanceof MetaGeneratedTool;
    }

    public static boolean isAE2EntityFloatingItem(EntityItem item) {
        return isAE2Installed && AE2FItem.isInstance(item);
    }

    @Optional.Method(modid = "gregtech_nh")
    public static String getHazmatArmorConfigKey(Hazard hazard) {
        return switch (hazard) {
            case ELECTRICAL -> "HazmatElectrical";
            case BIOLOGICAL -> "HazmatBiological";
            case FROST -> "HazmatFrost";
            case GAS -> "HazmatGas";
            case HEAT -> "HazmatHeat";
            case RADIOLOGICAL -> "HazmatRadiological";
            case SPACE -> "HazmatSpace";
        };
    }
}
