package com.brandon3055.draconicevolution.client.handler;

import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;

import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.common.handler.ConfigHandler;
import com.brandon3055.draconicevolution.common.handler.ContributorHandler;
import com.brandon3055.draconicevolution.common.items.armor.CustomArmorHandler;
import com.brandon3055.draconicevolution.common.items.armor.DraconicArmor;
import com.brandon3055.draconicevolution.common.items.armor.WyvernArmor;
import com.brandon3055.draconicevolution.common.items.weapons.BowHandler;
import com.brandon3055.draconicevolution.common.items.weapons.DraconicBow;
import com.brandon3055.draconicevolution.common.items.weapons.WyvernBow;
import com.brandon3055.draconicevolution.common.network.MountUpdatePacket;
import com.brandon3055.draconicevolution.common.utils.LogHelper;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Created by Brandon on 28/10/2014.
 */
public final class ClientEventHandler {

    private static final Random RANDOM = new Random();

    private int elapsedTicks;
    private float previousSensitivity = 0;
    private boolean bowZoom = false;
    private boolean lastTickBowZoom = false;
    private int tickSet = 0;
    private int remountTicksRemaining = 0;
    private int remountEntityID = 0;
    private float energyCrystalAlphaValue = 0f;
    private float energyCrystalAlphaTarget = 0f;

    @SubscribeEvent
    public void tickEnd(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }

        final Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld != null) {
            elapsedTicks++;

            if (bowZoom && !lastTickBowZoom) {
                previousSensitivity = mc.gameSettings.mouseSensitivity;
                mc.gameSettings.mouseSensitivity = previousSensitivity / 3;
            } else if (!bowZoom && lastTickBowZoom) {
                mc.gameSettings.mouseSensitivity = previousSensitivity;
            }

            lastTickBowZoom = bowZoom;
            if (elapsedTicks - tickSet > 10) bowZoom = false;

            if (energyCrystalAlphaValue < energyCrystalAlphaTarget) energyCrystalAlphaValue += 0.01f;
            if (energyCrystalAlphaValue > energyCrystalAlphaTarget) energyCrystalAlphaValue -= 0.01f;

            if (Math.abs(energyCrystalAlphaTarget - energyCrystalAlphaValue) <= 0.02f) {
                energyCrystalAlphaTarget = RANDOM.nextFloat();
            }

            searchForPlayerMount();
        }

    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void fovUpdate(FOVUpdateEvent event) {

        // region Bow FOV Update
        final ItemStack heldItem = event.entity.getHeldItem();
        if (heldItem != null && (heldItem.getItem() instanceof WyvernBow || heldItem.getItem() instanceof DraconicBow)
                && Minecraft.getMinecraft().gameSettings.keyBindUseItem.getIsKeyPressed()) {

            BowHandler.BowProperties properties = new BowHandler.BowProperties(heldItem, event.entity);

            event.newfov = ((6 - properties.zoomModifier) / 6) * event.fov;

            // if (ItemNBTHelper.getString(event.entity.getItemInUse(), "mode", "").equals("sharpshooter")){
            // if (event.entity.getItemInUse().getItem() instanceof WyvernBow) zMax = 1.35f;
            // else if (event.entity.getItemInUse().getItem() instanceof DraconicBow) zMax = 2.5f;
            // bowZoom = true;
            // tickSet = elapsedTicks;
            // }

        }
        // endregion

        // region Armor move speed FOV effect cancellation
        CustomArmorHandler.ArmorSummery summery = new CustomArmorHandler.ArmorSummery().getSummery(event.entity);

        if (summery != null && summery.speedModifier > 0) {
            IAttributeInstance iattributeinstance = event.entity
                    .getEntityAttribute(SharedMonsterAttributes.movementSpeed);
            float f = (float) ((iattributeinstance.getAttributeValue()
                    / (double) event.entity.capabilities.getWalkSpeed() + 1.0D) / 2.0D);
            event.newfov /= f;
        }

        // endregion
    }

    private void searchForPlayerMount() {
        if (remountTicksRemaining > 0) {
            Entity e = Minecraft.getMinecraft().theWorld.getEntityByID(remountEntityID);
            if (e != null) {
                Minecraft.getMinecraft().thePlayer.mountEntity(e);
                LogHelper.info("Successfully placed player on mount after " + (500 - remountTicksRemaining) + " ticks");
                remountTicksRemaining = 0;
                return;
            }
            remountTicksRemaining--;
            if (remountTicksRemaining == 0) {
                LogHelper.error("Unable to locate player mount after 500 ticks! Aborting");
                DraconicEvolution.network.sendToServer(new MountUpdatePacket(-1));
            }
        }
    }

    public void tryRepositionPlayerOnMount(int id) {
        if (remountTicksRemaining == 500) return;
        remountTicksRemaining = 500;
        remountEntityID = id;
        LogHelper.info("Started checking for player mount");
    }

    @SubscribeEvent
    public void renderPlayerEvent(RenderPlayerEvent.Specials.Post event) {
        ContributorHandler.render(event);
    }

    @SubscribeEvent
    public void renderArmorEvent(RenderPlayerEvent.SetArmorModel event) {
        if (ConfigHandler.useOriginal3DArmorModel || ConfigHandler.useOldArmorModel) return;
        if (event.stack != null
                && (event.stack.getItem() instanceof DraconicArmor || event.stack.getItem() instanceof WyvernArmor)) {
            ItemArmor itemarmor = (ItemArmor) event.stack.getItem();
            ModelBiped modelbiped = itemarmor.getArmorModel(event.entityPlayer, event.stack, event.slot);
            event.renderer.setRenderPassModel(modelbiped);
            modelbiped.onGround = event.renderer.modelBipedMain.onGround;
            modelbiped.isRiding = event.renderer.modelBipedMain.isRiding;
            modelbiped.isChild = event.renderer.modelBipedMain.isChild;
            event.result = 1;
        }
    }

    public int getElapsedTicks() {
        return elapsedTicks;
    }

    public float getEnergyCrystalAlpha() {
        return energyCrystalAlphaValue;
    }
}
