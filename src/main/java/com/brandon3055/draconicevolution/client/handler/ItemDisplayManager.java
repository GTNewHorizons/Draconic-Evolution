package com.brandon3055.draconicevolution.client.handler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public final class ItemDisplayManager {

    private static final RenderItem renderItem = new RenderItem();
    private static final FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
    private static final TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();

    private final int ticks;
    private ItemStack itemStack;
    private int ticksCounter;

    public ItemDisplayManager(int ticks) {
        this.ticks = ticks;
    }

    public void startDrawing(ItemStack itemStack) {
        this.itemStack = itemStack == null ? null : itemStack.copy();
        ticksCounter = ticks;
    }

    @SubscribeEvent
    public void tickEnd(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            if (--ticksCounter == 0) {
                itemStack = null;
            }
        }
    }

    @SubscribeEvent
    public void renderGameOverlayEvent(RenderGameOverlayEvent.Post event) {
        if (event.type == RenderGameOverlayEvent.ElementType.ALL) {
            if (ticksCounter > 0 && itemStack != null && itemStack.getItem() != null) {
                GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
                GL11.glEnable(GL12.GL_RESCALE_NORMAL);
                RenderHelper.enableGUIStandardItemLighting();

                GL11.glPushMatrix();
                final int y = event.resolution.getScaledHeight();
                GL11.glTranslatef(7.0f, y * 0.25f, 0);
                renderItem.renderItemAndEffectIntoGUI(fontRenderer, textureManager, itemStack, 0, 0);
                GL11.glPopMatrix();

                RenderHelper.disableStandardItemLighting();
                GL11.glDisable(GL12.GL_RESCALE_NORMAL);
                GL11.glDisable(GL11.GL_BLEND);
            }
        }
    }
}
