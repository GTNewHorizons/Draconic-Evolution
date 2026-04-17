package com.brandon3055.draconicevolution.client.handler;

import static com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil.formatNumber;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import org.lwjgl.opengl.GL11;

import com.brandon3055.draconicevolution.client.gui.GuiHudConfig;
import com.brandon3055.draconicevolution.client.utils.GuiHelper;
import com.brandon3055.draconicevolution.common.handler.ConfigHandler;
import com.brandon3055.draconicevolution.common.items.armor.CustomArmorHandler.ArmorSummary;
import com.brandon3055.draconicevolution.common.utils.IHudDisplayBlock;
import com.brandon3055.draconicevolution.common.utils.IHudDisplayItem;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Created by Brandon on 26/01/2015.
 */
public final class HudHandler {

    private List<String> hudList = null;
    private List<String> ltHudList = null;
    private float toolTipFadeOut = 0F;
    private float armorStatsFadeOut = 0F;
    private boolean showShieldHud = false;
    private int shieldPercentCharge = 0;
    private float shieldPoints = 0F;
    private float maxShieldPoints = 0F;
    private float shieldEntropy = 0F;
    private int rfCharge = 0;
    private long rfTotal = 0;

    @SubscribeEvent
    public void drawHUD(RenderGameOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.type != RenderGameOverlayEvent.ElementType.ALL || mc.gameSettings.showDebugInfo
                || mc.currentScreen instanceof GuiChat) {
            return;
        }

        ScaledResolution resolution = event.resolution;
        final int width = resolution.getScaledWidth();
        final int height = resolution.getScaledHeight();
        FontRenderer fontRenderer = mc.fontRenderer;

        if (ConfigHandler.hudSettings[10] == 1 && hudList != null && toolTipFadeOut > 0) {
            int x = (int) (((float) ConfigHandler.hudSettings[0] / 1000F) * (float) width);
            int y = (int) (((float) ConfigHandler.hudSettings[1] / 1000F) * (float) height);

            GL11.glPushMatrix();
            GuiHelper.drawHoveringText(
                    hudList,
                    x,
                    y,
                    fontRenderer,
                    toolTipFadeOut > 1 ? 1 : toolTipFadeOut,
                    ConfigHandler.hudSettings[4] / 100D,
                    width,
                    height);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glPopMatrix();
        }

        if (ConfigHandler.hudSettings[11] == 1 && showShieldHud) {
            int x = (int) (((float) ConfigHandler.hudSettings[2] / 1000F) * (float) width);
            int y = (int) (((float) ConfigHandler.hudSettings[3] / 1000F) * (float) height);

            drawArmorHUD(x, y, ConfigHandler.hudSettings[8] == 1, ConfigHandler.hudSettings[5] / 100D);
        }
    }

    @SubscribeEvent
    public void tickEnd(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && Minecraft.getMinecraft().theWorld != null) {
            this.clientTick();
        }
    }

    private void clientTick() {
        if (ConfigHandler.hudSettings[6] > 0 && toolTipFadeOut > 1F - ((float) ConfigHandler.hudSettings[6] * 0.25F)) {
            toolTipFadeOut -= 0.1F;
        }
        if (hudList != null && !hudList.equals(ltHudList)) {
            toolTipFadeOut = 5F;
        }
        if (ConfigHandler.hudSettings[7] > 0
                && armorStatsFadeOut > 1F - ((float) ConfigHandler.hudSettings[7] * 0.25F)) {
            armorStatsFadeOut -= 0.1F;
            if (armorStatsFadeOut < 0) armorStatsFadeOut = 0;
        }

        ltHudList = hudList;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;

        hudList = null;

        if (mc.currentScreen != null) {
            if (mc.currentScreen instanceof GuiHudConfig) {
                hudList = new ArrayList<>();
                hudList.add(StatCollector.translateToLocal("info.de.hudDisplayConfigTxt1.txt"));
                hudList.add("");
                hudList.add("");
                hudList.add("");
                hudList.add(StatCollector.translateToLocal("info.de.hudDisplayConfigTxt3.txt"));
                toolTipFadeOut = 1F;
                armorStatsFadeOut = 1F;
            }
        } else
            if (mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof IHudDisplayItem) {
                hudList = ((IHudDisplayItem) mc.thePlayer.getHeldItem().getItem())
                        .getDisplayData(mc.thePlayer.getHeldItem());
            }

        MovingObjectPosition mop = mc.thePlayer.rayTrace(5, 0);
        if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                && mc.theWorld.getBlock(mop.blockX, mop.blockY, mop.blockZ) instanceof IHudDisplayBlock) {
            hudList = ((IHudDisplayBlock) mc.theWorld.getBlock(mop.blockX, mop.blockY, mop.blockZ))
                    .getDisplayData(mc.theWorld, mop.blockX, mop.blockY, mop.blockZ);
        }

        final ArmorSummary summary = ArmorSummary.get(mc.thePlayer);

        if (summary == null) {
            showShieldHud = false;
            return;
        }
        showShieldHud = armorStatsFadeOut > 0F;

        if (maxShieldPoints != summary.maxProtectionPoints || shieldPoints != summary.protectionPoints
                || shieldEntropy != summary.entropy
                || rfTotal != summary.totalEnergyStored)
            armorStatsFadeOut = 5F;

        maxShieldPoints = summary.maxProtectionPoints;
        shieldPoints = summary.protectionPoints;
        shieldPercentCharge = (int) (summary.protectionPoints / summary.maxProtectionPoints * 100D);
        shieldEntropy = summary.entropy;
        rfCharge = (int) ((double) summary.totalEnergyStored / Math.max((double) summary.maxTotalEnergyStorage, 1D)
                * 100D);
        rfTotal = summary.totalEnergyStored;
    }

    private void drawArmorHUD(int x, int y, boolean rotated, double scale) {
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        ResourceHandler.bindResource("textures/gui/HUD.png");

        GL11.glTranslated(x, y, 0);
        GL11.glScaled(scale, scale, 1);
        GL11.glTranslated(-x, -y, 0);
        GL11.glColor4f(1F, 1F, 1F, Math.min(armorStatsFadeOut, 1F));

        if (rotated) {
            GuiHelper.drawTexturedRect(x - 15, y + 1, 14, 16, 2, 0, 13, 15, 0, GuiHelper.PXL128);
            x += 104;
            GL11.glTranslated(x, y, 0);
            GL11.glRotated(-90, 0, 0, -1);
            GL11.glTranslated(-x, -y, 0);
        } else GuiHelper.drawTexturedRect(x + 1, y + 105, 15, 17, 2, 0, 13, 15, 0, GuiHelper.PXL128);

        GuiHelper.drawTexturedRect(x, y, 17, 104, 0, 15, 17, 104, 0, GuiHelper.PXL128);
        GuiHelper.drawTexturedRect(
                x + 2,
                y + 2 + (100 - shieldPercentCharge),
                7,
                shieldPercentCharge,
                17,
                100 - shieldPercentCharge,
                7,
                shieldPercentCharge,
                0,
                GuiHelper.PXL128);
        GuiHelper.drawTexturedRect(
                x + 10,
                y + 2 + 100 - (int) shieldEntropy,
                2,
                (int) shieldEntropy,
                25,
                100 - (int) shieldEntropy,
                2,
                (int) shieldEntropy,
                0,
                GuiHelper.PXL128);
        GuiHelper.drawTexturedRect(
                x + 13,
                y + 2 + 100 - rfCharge,
                2,
                rfCharge,
                28,
                100 - rfCharge,
                2,
                rfCharge,
                0,
                GuiHelper.PXL128);

        if (ConfigHandler.hudSettings[9] == 1) {
            FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
            GL11.glTranslated(x, y, 0);
            if (rotated) GL11.glRotated(90, 0, 0, -1);
            GL11.glTranslated(-x, -y, 0);
            String shield = Math.round(shieldPoints) + "/" + (int) maxShieldPoints;
            String entropy = "EN: " + (int) shieldEntropy + "%";
            String energy = "RF: " + formatNumber(rfTotal);
            float fade = Math.min(armorStatsFadeOut, 1F);
            if (!rotated) {
                fontRenderer
                        .drawStringWithShadow(shield, x + 18, y + 74, ((int) (fade * 240F) + 0x10 << 24) | 0x00FFFFFF);
                fontRenderer
                        .drawStringWithShadow(energy, x + 18, y + 84, ((int) (fade * 240F) + 0x10 << 24) | 0x00FFFFFF);
                fontRenderer
                        .drawStringWithShadow(entropy, x + 18, y + 94, ((int) (fade * 240F) + 0x10 << 24) | 0x00FFFFFF);
            } else {
                fontRenderer.drawString(
                        shield,
                        x - 52 - fontRenderer.getStringWidth(shield) / 2,
                        y + 2,
                        ((int) (fade * 240F) + 0x10 << 24) | 0x000000FF);
                fontRenderer.drawStringWithShadow(
                        entropy,
                        x - fontRenderer.getStringWidth(entropy),
                        y + 18,
                        ((int) (fade * 240F) + 0x10 << 24) | 0x00FFFFFF);
                fontRenderer
                        .drawStringWithShadow(energy, x - 102, y + 18, ((int) (fade * 240F) + 0x10 << 24) | 0x00FFFFFF);
            }
        }

        ResourceHandler.bindTexture(ResourceHandler.getResourceWOP("minecraft:textures/gui/icons.png"));
        GL11.glColor4f(1F, 1F, 1F, 1F);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glPopMatrix();
    }
}

// GL11.glPushMatrix();
// GL11.glEnable(GL11.GL_ALPHA_TEST);
// ResourceHandler.bindResource("textures/gui/HUD.png");
//
// if (rotated){
// GuiHelper.drawTexturedRect(x-(12*scale), y+scale, (int)(11*scale), (int)(13*scale), 2, 0, 11, 13, 0,
// GuiHelper.PXL128);
// x+=(104*scale);
// GL11.glTranslated(x, y, 0);
// GL11.glRotated(-90, 0, 0, -1);
// GL11.glTranslated(-x, -y, 0);
// }
// else GuiHelper.drawTexturedRect(x+(1*scale), y+(104*scale)+1, (int)(13*scale), (int)(15*scale), 2, 0, 11, 13, 0,
// GuiHelper.PXL128);
//
// GuiHelper.drawTexturedRect(x, y, (int)(17*scale), (int)(104*scale), 0, 15, 17, 104, 0, GuiHelper.PXL128);
// GuiHelper.drawTexturedRect(x+(2*scale), y+(2*scale)+(100-shieldPercentCharge), (int)(7*scale), shieldPercentCharge
// *scale, 17, 100-shieldPercentCharge, 7, shieldPercentCharge, 0, GuiHelper.PXL128);
// GuiHelper.drawTexturedRect(x+(10*scale), y+(2+(100-(int)shieldEntropy) * scale), (int)(2*scale), (int)shieldEntropy
// * scale, 25, 100-(int)shieldEntropy, 2, (int)shieldEntropy, 0, GuiHelper.PXL128);
//
// if (ConfigHandler.hudSettings[9] == 1) {
// FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
// GL11.glTranslated(x, y, 0);
// GL11.glScaled(scale, scale, 1);
// if (rotated) GL11.glRotated(90, 0, 0, -1);
// GL11.glTranslated(-x, -y, 0);
// if (!rotated) {
// fontRenderer.drawStringWithShadow(Math.round(shieldPoints) + "/" + (int)maxShieldPoints, x + 16, y + 84, 0xFFFFFF);
// fontRenderer.drawStringWithShadow((int)shieldEntropy + "%", x + 16, y + 94, 0xFFFFFF);
// }else {
// fontRenderer.drawStringWithShadow(Math.round(shieldPoints) + "/" + (int)maxShieldPoints, x - 102, y + 16, 0xFFFFFF);
// fontRenderer.drawStringWithShadow((int)shieldEntropy + "%", x - fontRenderer.getStringWidth((int)shieldEntropy +
// "%"), y + 16, 0xFFFFFF);
// }
// }
//
// ResourceHandler.bindTexture(ResourceHandler.getResourceWOP("minecraft:textures/gui/icons.png"));
// GL11.glDisable(GL11.GL_ALPHA_TEST);
// GL11.glPopMatrix();
