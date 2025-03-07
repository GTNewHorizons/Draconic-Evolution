package com.brandon3055.draconicevolution.integration;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

import com.brandon3055.draconicevolution.common.items.armor.CustomArmorHandler.ArmorSummery;
import com.brandon3055.draconicevolution.common.utills.LogHelper;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Created by brandon3055 on 29/9/2015.
 */
public class ModHelper {

    private static final boolean isTConInstalled;
    private static final boolean isAvaritiaInstalled;
    // private static boolean isRotaryCraftInstalled;
    private static final boolean isGregTechInstalled;
    private static final boolean isBartworkdsInstalled;
    private static final boolean isAE2Installed;

    private static Item cleaver;
    private static Item avaritiaSword;
    // private static Item bedrockSword;

    private static Class<?> bwores;
    private static Class<?> GTores;
    private static Class<?> AE2FItem;

    static {
        isTConInstalled = Loader.isModLoaded("TConstruct");
        isAvaritiaInstalled = Loader.isModLoaded("Avaritia");
        // isRotaryCraftInstalled = Loader.isModLoaded("RotaryCraft");
        isGregTechInstalled = Loader.isModLoaded("gregtech");
        isBartworkdsInstalled = Loader.isModLoaded("bartworks");
        isAE2Installed = Loader.isModLoaded("appliedenergistics2");
        final String GT_ORE_CLASS = "gregtech.common.blocks.TileEntityOres";
        final String BW_ORE_CLASS = "bartworks.system.material.BWMetaGeneratedOres";
        final String AE2_FITEM_CLASS = "appeng.entity.EntityFloatingItem";
        if (isGregTechInstalled) try {
            GTores = Class.forName(GT_ORE_CLASS);
        } catch (ClassNotFoundException e) {
            LogHelper.error("Couldn't reflect class " + GT_ORE_CLASS);
        }
        if (isBartworkdsInstalled) try {
            bwores = Class.forName(BW_ORE_CLASS);
        } catch (ClassNotFoundException e) {
            LogHelper.error("Couldn't reflect class " + BW_ORE_CLASS);
        }
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

    // public static boolean isHoldingBedrockSword(EntityPlayer player) {
    // if (!isRotaryCraftInstalled) return false;
    // else if (bedrockSword == null) bedrockSword = GameRegistry.findItem("RotaryCraft",
    // "rotarycraft_item_bedsword");
    //
    // return bedrockSword != null && player.getHeldItem() != null &&
    // player.getHeldItem().getItem().equals(bedrockSword);
    // }

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
        }
        // else if (isHoldingBedrockSword(attacker)) {
        // summery.entropy += 10;
        //
        // if (summery.entropy > 100) {
        // summery.entropy = 100;
        // }
        //
        // return Math.max(event.ammount, Math.min(50F, summery.protectionPoints));
        // }
        else if (event.source.isUnblockable() || event.source.canHarmInCreative()) {
            summery.entropy += 3;

            if (summery.entropy > 100) {
                summery.entropy = 100;
            }

            return event.ammount * 2;
        }

        return event.ammount;
    }

    public static boolean isGregTechTileEntityOre(TileEntity te) {
        return isGregTechInstalled && GTores.isInstance(te) || isBartworkdsInstalled && bwores.isInstance(te);
    }

    public static boolean isAE2EntityFloatingItem(EntityItem item) {
        return isAE2Installed && AE2FItem.isInstance(item);
    }
}
