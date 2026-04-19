package com.brandon3055.draconicevolution.client.handler;

import java.util.HashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.model.AdvancedModelLoader;
import net.minecraftforge.client.model.IModelCustom;
import net.minecraftforge.event.world.WorldEvent;

import org.lwjgl.opengl.GL11;

import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.common.utils.DataUtils;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public final class ShieldRenderHandler {

    private static final IModelCustom SHIELD_SPHERE = AdvancedModelLoader
            .loadModel(ResourceHandler.getResource("models/shieldSphere.obj"));

    private final HashMap<EntityPlayer, DataUtils.XZPair<Float, Integer>> shieldStatus = new HashMap<>();

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.world.isRemote) {
            this.shieldStatus.clear();
        }
    }

    @SubscribeEvent
    public void tickEnd(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !shieldStatus.isEmpty()) {
            final int ticks = DraconicEvolution.clientProxy().getElapsedTicks();
            shieldStatus.entrySet().removeIf(entry -> ticks - entry.getValue().getValue() > 5);
        }
    }

    @SubscribeEvent
    public void renderPlayerEvent(RenderPlayerEvent.Post event) {
        final DataUtils.XZPair<Float, Integer> pair = shieldStatus.get(event.entityPlayer);
        if (pair != null) {
            GL11.glPushMatrix();
            GL11.glDepthMask(false);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_LIGHTING);
            ResourceHandler.bindResource("textures/models/shieldSphere.png");

            final float p = pair.getKey();
            final EntityPlayer viewingPlayer = Minecraft.getMinecraft().thePlayer;
            final int ticks = DraconicEvolution.clientProxy().getElapsedTicks();
            final int i = 5 - (ticks - pair.getValue());

            GL11.glColor4f(1F - p, 0F, p, i / 5F);

            if (viewingPlayer != event.entityPlayer) {
                double translationXLT = event.entityPlayer.prevPosX - viewingPlayer.prevPosX;
                double translationYLT = event.entityPlayer.prevPosY - viewingPlayer.prevPosY;
                double translationZLT = event.entityPlayer.prevPosZ - viewingPlayer.prevPosZ;

                double translationX = translationXLT
                        + (((event.entityPlayer.posX - viewingPlayer.posX) - translationXLT) * event.partialRenderTick);
                double translationY = translationYLT
                        + (((event.entityPlayer.posY - viewingPlayer.posY) - translationYLT) * event.partialRenderTick);
                double translationZ = translationZLT
                        + (((event.entityPlayer.posZ - viewingPlayer.posZ) - translationZLT) * event.partialRenderTick);

                GL11.glTranslated(translationX, translationY + 1.1, translationZ);
            } else {
                GL11.glTranslated(0, -0.5, 0);
            }

            GL11.glScaled(1, 1.5, 1);

            SHIELD_SPHERE.renderAll();

            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glDepthMask(true);
            GL11.glPopMatrix();
        }
    }

    public void renderShield(EntityPlayer player, float shieldPowerF) {
        final int ticks = DraconicEvolution.clientProxy().getElapsedTicks();
        this.shieldStatus.put(player, new DataUtils.XZPair<>(shieldPowerF, ticks));
    }
}
