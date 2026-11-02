package com.brandon3055.draconicevolution.client.handler;

import java.util.Arrays;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.brandon3055.draconicevolution.client.utils.GuiHelper;
import com.brandon3055.draconicevolution.common.ModItems;
import com.brandon3055.draconicevolution.common.handler.ConfigHandler;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.gtnewhorizons.dashboard.api.HudBounds;
import com.gtnewhorizons.dashboard.api.HudElement;
import com.gtnewhorizons.dashboard.api.HudRegistry;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Lets players move, resize and hide the info box, armor HUD and magnet icon in Dashboard's HUD editor. The only class
 * using Dashboard, so only call it while Dashboard is loaded. The info box and armor HUD are placed by Draconic's own
 * HUD settings, as thousandths of the screen, so their default bounds are worked out the same way.
 */
@SideOnly(Side.CLIENT)
public final class DashboardHud {

    // Indexes into ConfigHandler.hudSettings
    private static final int INFO_X = 0;
    private static final int INFO_Y = 1;
    private static final int ARMOR_X = 2;
    private static final int ARMOR_Y = 3;
    private static final int INFO_SCALE = 4;
    private static final int ARMOR_SCALE = 5;
    private static final int ARMOR_SIDEWAYS = 8;
    private static final int ARMOR_TEXT = 9;
    private static final int INFO_ENABLED = 10;
    private static final int ARMOR_ENABLED = 11;
    private static final int ITEM_SIZE = 16;
    private static final int MAGNET_X = 7;
    private static final String OWN_SETTINGS = "dashboard.element.draconicevolution.own_settings";

    private static final HudElement INFO_BOX = new HudElement(
            "draconicevolution:info_box",
            DashboardHud::getInfoBoxBounds).setOwnSettings(OWN_SETTINGS, "key.toolConfig")
                    .setActiveCheck(() -> ConfigHandler.hudSettings[INFO_ENABLED] == 1);

    private static final HudElement ARMOR_HUD = new HudElement(
            "draconicevolution:armor_hud",
            DashboardHud::getArmorHudBounds).setOwnSettings(OWN_SETTINGS, "key.toolConfig")
                    .setActiveCheck(() -> ConfigHandler.hudSettings[ARMOR_ENABLED] == 1);

    private static final HudElement MAGNET_ICON = new HudElement(
            "draconicevolution:magnet_icon",
            (screenWidth, screenHeight) -> new HudBounds(MAGNET_X, (int) (screenHeight * 0.25f), ITEM_SIZE, ITEM_SIZE));

    private static final RenderItem RENDER_ITEM = new RenderItem();

    private static HudHandler hudHandler;

    private DashboardHud() {}

    /** The handler holds the info box lines and armor values. */
    public static void register(HudHandler handler) {
        hudHandler = handler;
        INFO_BOX.setPreview(
                (screenWidth, screenHeight) -> HudRegistry
                        .render(INFO_BOX, () -> drawInfoBoxPreview(screenWidth, screenHeight)));
        ARMOR_HUD.setPreview(
                (screenWidth, screenHeight) -> HudRegistry
                        .render(ARMOR_HUD, () -> drawArmorHudPreview(screenWidth, screenHeight)));
        MAGNET_ICON.setPreview(
                (screenWidth, screenHeight) -> HudRegistry
                        .render(MAGNET_ICON, () -> drawMagnetIconPreview(screenHeight)));
        HudRegistry.register(INFO_BOX);
        HudRegistry.register(ARMOR_HUD);
        HudRegistry.register(MAGNET_ICON);
    }

    /** Thousandths of the screen, like Draconic's position settings. */
    private static int getPosition(int index, int screenSize) {
        return (int) (ConfigHandler.hudSettings[index] / 1000f * screenSize);
    }

    /** The lines showing now, or Draconic's own sample from its HUD settings screen. */
    private static List<String> getInfoLines() {
        if (hudHandler.hudList != null && !hudHandler.hudList.isEmpty() && hudHandler.toolTipFadeOut > 0) {
            return hudHandler.hudList;
        }
        return Arrays.asList(
                StatCollector.translateToLocal("info.de.hudDisplayConfigTxt1.txt"),
                "",
                StatCollector.translateToLocal("info.de.hudDisplayConfigTxt2.txt"),
                "",
                StatCollector.translateToLocal("info.de.hudDisplayConfigTxt3.txt"));
    }

    /** Like Minecraft's tooltip, which the box copies: next to its spot, kept on screen, in its scale. */
    private static HudBounds getInfoBoxBounds(int screenWidth, int screenHeight) {
        List<String> lines = getInfoLines();
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        double scale = ConfigHandler.hudSettings[INFO_SCALE] / 100d;
        int scaledWidth = (int) (screenWidth / scale);
        int scaledHeight = (int) (screenHeight / scale);
        int textWidth = 0;
        for (String line : lines) {
            textWidth = Math.max(textWidth, font.getStringWidth(line));
        }
        int textHeight = 8 + (lines.size() > 1 ? 2 + (lines.size() - 1) * 10 : 0);
        int textX = (int) (getPosition(INFO_X, screenWidth) / scale) + 12;
        int textY = (int) (getPosition(INFO_Y, screenHeight) / scale) - 12;
        if (textX + textWidth > scaledWidth) {
            textX -= 28 + textWidth;
        }
        if (textY + textHeight + 6 > scaledHeight) {
            textY = scaledHeight - textHeight - 6;
        }
        return new HudBounds(
                (int) Math.round((textX - 4) * scale),
                (int) Math.round((textY - 4) * scale),
                (int) Math.round((textWidth + 8) * scale),
                (int) Math.round((textHeight + 8) * scale));
    }

    /** Upright: the bar with its icon below and its text on the right. Sideways: the icon on the left, text below. */
    private static HudBounds getArmorHudBounds(int screenWidth, int screenHeight) {
        double scale = ConfigHandler.hudSettings[ARMOR_SCALE] / 100d;
        boolean text = ConfigHandler.hudSettings[ARMOR_TEXT] == 1;
        int left;
        int width;
        int height;
        if (ConfigHandler.hudSettings[ARMOR_SIDEWAYS] == 1) {
            left = -15;
            width = 119;
            height = text ? 27 : 17;
        } else {
            left = 0;
            width = text ? Math.max(17, 18 + getArmorTextWidth()) : 17;
            height = 122;
        }
        return new HudBounds(
                getPosition(ARMOR_X, screenWidth) + (int) Math.round(left * scale),
                getPosition(ARMOR_Y, screenHeight),
                (int) Math.round(width * scale),
                (int) Math.round(height * scale));
    }

    /** The three text lines next to the upright bar, with the values showing now or the preview's. */
    private static int getArmorTextWidth() {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        boolean showing = hudHandler.maxShieldPoints > 0 || hudHandler.rfTotal > 0;
        float points = showing ? hudHandler.shieldPoints : 700;
        float maxPoints = showing ? hudHandler.maxShieldPoints : 1000;
        float entropy = showing ? hudHandler.shieldEntropy : 20;
        long energy = showing ? hudHandler.rfTotal : 1_000_000L;
        return Math.max(
                font.getStringWidth(Math.round(points) + "/" + (int) maxPoints),
                Math.max(
                        font.getStringWidth("EN: " + (int) entropy + "%"),
                        font.getStringWidth("RF: " + NumberFormatUtil.formatNumber(energy))));
    }

    private static void drawInfoBoxPreview(int screenWidth, int screenHeight) {
        GuiHelper.drawHoveringText(
                getInfoLines(),
                getPosition(INFO_X, screenWidth),
                getPosition(INFO_Y, screenHeight),
                Minecraft.getMinecraft().fontRenderer,
                1f,
                ConfigHandler.hudSettings[INFO_SCALE] / 100d,
                screenWidth,
                screenHeight);
    }

    /** Draconic's own drawing, with sample values put in the handler for the moment. */
    private static void drawArmorHudPreview(int screenWidth, int screenHeight) {
        HudHandler handler = hudHandler;
        float fade = handler.armorStatsFadeOut;
        int charge = handler.shieldPercentCharge;
        float points = handler.shieldPoints;
        float maxPoints = handler.maxShieldPoints;
        float entropy = handler.shieldEntropy;
        int energyCharge = handler.rfCharge;
        long energy = handler.rfTotal;
        handler.armorStatsFadeOut = 1f;
        handler.shieldPercentCharge = 70;
        handler.shieldPoints = 700f;
        handler.maxShieldPoints = 1000f;
        handler.shieldEntropy = 20f;
        handler.rfCharge = 60;
        handler.rfTotal = 1_000_000L;
        try {
            handler.drawArmorHUD(
                    getPosition(ARMOR_X, screenWidth),
                    getPosition(ARMOR_Y, screenHeight),
                    ConfigHandler.hudSettings[ARMOR_SIDEWAYS] == 1,
                    ConfigHandler.hudSettings[ARMOR_SCALE] / 100d);
        } finally {
            handler.armorStatsFadeOut = fade;
            handler.shieldPercentCharge = charge;
            handler.shieldPoints = points;
            handler.maxShieldPoints = maxPoints;
            handler.shieldEntropy = entropy;
            handler.rfCharge = energyCharge;
            handler.rfTotal = energy;
        }
    }

    private static void drawMagnetIconPreview(int screenHeight) {
        Minecraft mc = Minecraft.getMinecraft();
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        RenderHelper.enableGUIStandardItemLighting();
        RENDER_ITEM.renderItemAndEffectIntoGUI(
                mc.fontRenderer,
                mc.getTextureManager(),
                new ItemStack(ModItems.magnet),
                MAGNET_X,
                (int) (screenHeight * 0.25f));
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
    }
}
