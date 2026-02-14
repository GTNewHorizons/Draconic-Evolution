package com.brandon3055.draconicevolution.client.gui.componentguis;

import static com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil.formatNumber;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import com.brandon3055.draconicevolution.client.handler.ResourceHandler;
import com.brandon3055.draconicevolution.client.utils.GuiHelper;
import com.brandon3055.draconicevolution.client.utils.guicomponents.ComponentButton;
import com.brandon3055.draconicevolution.client.utils.guicomponents.ComponentCollection;
import com.brandon3055.draconicevolution.client.utils.guicomponents.ComponentTextureButton;
import com.brandon3055.draconicevolution.client.utils.guicomponents.ComponentTexturedRect;
import com.brandon3055.draconicevolution.client.utils.guicomponents.GUIBase;
import com.brandon3055.draconicevolution.common.container.ContainerReactor;
import com.brandon3055.draconicevolution.common.handler.ConfigHandler;
import com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.reactor.TileReactorCore;
import com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.reactor.TileReactorCore.ReactorState;
import com.brandon3055.draconicevolution.common.utils.Utils;

/**
 * Created by brandon3055 on 30/7/2015.
 */
public class GUIReactor extends GUIBase {

    private static boolean showStatistics = false;
    private final TileReactorCore core;
    private final ContainerReactor container;

    public GUIReactor(TileReactorCore core, ContainerReactor container) {
        super(container, 248, 222);
        this.core = core;
        this.container = container;
    }

    @Override
    protected ComponentCollection assembleComponents() {
        final ResourceLocation widgets = ResourceHandler.getResource("textures/gui/Widgets.png");
        collection = new ComponentCollection(0, 0, 248, 222, this);
        collection.addComponent(
                new ComponentTexturedRect(0, 0, xSize, ySize, ResourceHandler.getResource("textures/gui/Reactor.png")));
        collection.addComponent(
                new ComponentTextureButton(
                        14,
                        190,
                        18,
                        162,
                        18,
                        18,
                        0,
                        this,
                        "",
                        StatCollector.translateToLocal("button.de.reactorCharge.txt"),
                        widgets))
                .setName("CHARGE");
        collection.addComponent(
                new ComponentTextureButton(
                        14,
                        190,
                        18,
                        54,
                        18,
                        18,
                        1,
                        this,
                        "",
                        StatCollector.translateToLocal("button.de.reactorStart.txt"),
                        widgets))
                .setName("ACTIVATE");
        collection.addComponent(
                new ComponentTextureButton(
                        216,
                        190,
                        18,
                        108,
                        18,
                        18,
                        2,
                        this,
                        "",
                        StatCollector.translateToLocal("button.de.reactorStop.txt"),
                        widgets))
                .setName("DEACTIVATE");
        collection.addComponent(
                new ComponentButton(
                        9,
                        120,
                        43,
                        15,
                        3,
                        this,
                        StatCollector.translateToLocal("button.de.stats.txt"),
                        StatCollector.translateToLocal("button.de.statsShow.txt")))
                .setName("STATS");
        return collection;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
        // Draw I/O Slots
        if (core.reactorState == ReactorState.OFFLINE) {
            RenderHelper.enableGUIStandardItemLighting();
            drawTexturedModalRect(guiLeft + 14, guiTop + 139, 14, ySize, 18, 18);
            drawTexturedModalRect(guiLeft + 216, guiTop + 139, 32, ySize, 18, 18);

            fontRendererObj
                    .drawString(StatCollector.translateToLocal("gui.de.insert.txt"), guiLeft + 8, guiTop + 159, 0);
            fontRendererObj
                    .drawString(StatCollector.translateToLocal("gui.de.fuel.txt"), guiLeft + 13, guiTop + 168, 0);

            fontRendererObj
                    .drawString(StatCollector.translateToLocal("gui.de.extract.txt"), guiLeft + 206, guiTop + 159, 0);
            fontRendererObj
                    .drawString(StatCollector.translateToLocal("gui.de.fuel.txt"), guiLeft + 215, guiTop + 168, 0);
        }
        drawCenteredString(fontRendererObj, "Draconic Reactor", guiLeft + xSize / 2, guiTop + 4, 0x00FFFF);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
        ResourceHandler.bindResource("textures/gui/Reactor.png");

        GL11.glColor4f(1f, 1f, 1f, 1f);
        // Draw Indicators
        double value = Math.min(core.reactionTemperature, core.maxReactTemperature) / core.maxReactTemperature;
        int pixOffset = (int) (value * 108);
        drawTexturedModalRect(11, 112 - pixOffset, 0, 222, 14, 5);

        value = core.fieldCharge / core.maxFieldCharge;
        pixOffset = (int) (value * 108);
        drawTexturedModalRect(35, 112 - pixOffset, 0, 222, 14, 5);

        value = (double) core.energySaturation / (double) core.maxEnergySaturation;
        pixOffset = (int) (value * 108);
        drawTexturedModalRect(199, 112 - pixOffset, 0, 222, 14, 5);

        value = ((double) core.convertedFuel) / ((double) core.reactorFuel + (double) core.convertedFuel);
        pixOffset = (int) (value * 108);
        drawTexturedModalRect(223, 112 - pixOffset, 0, 222, 14, 5);

        GL11.glColor4f(1F, 1F, 1F, 1F);
        if (showStatistics) {
            ResourceHandler.bindResource("textures/gui/Reactor.png");
            for (int i = 1; i <= 10; i++) {
                drawTexturedModalRect(63, i * 12, 0, 240, 122, 16);
            }
            drawStatistics();
        } else {
            GL11.glPushMatrix();
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GL11.glTranslated(124, 71, 100);
            double scale = 100 / (core.getCoreDiameter());
            GL11.glScaled(scale, scale, scale);
            GL11.glColor4f(1F, 1F, 1F, 1F);
            GL11.glDisable(GL11.GL_CULL_FACE);

            TileEntityRendererDispatcher.instance.renderTileEntityAt(core, -0.5D, -0.5D, -0.5D, 0.0F);

            GL11.glPopAttrib();
            GL11.glPopMatrix();
            drawStatus();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        List<String> text = new ArrayList<>();
        if (GuiHelper.isInRect(9, 4, 18, 114, mouseX - guiLeft, mouseY - guiTop)) {
            text.add(StatCollector.translateToLocal("gui.de.reactionTemp.txt"));
            text.add((int) core.reactionTemperature + "C");
            drawHoveringText(text, mouseX, mouseY, fontRendererObj);
        } else if (GuiHelper.isInRect(33, 4, 18, 114, mouseX - guiLeft, mouseY - guiTop)) {
            text.add(StatCollector.translateToLocal("gui.de.fieldStrength.txt"));
            if (core.maxFieldCharge > 0) {
                text.add(Utils.round(core.fieldCharge / core.maxFieldCharge * 100D, 100D) + "%");
            }
            text.add(formatNumber((int) core.fieldCharge) + " / " + formatNumber((int) core.maxFieldCharge));
            drawHoveringText(text, mouseX, mouseY, fontRendererObj);
        } else if (GuiHelper.isInRect(197, 4, 18, 114, mouseX - guiLeft, mouseY - guiTop)) {
            text.add(StatCollector.translateToLocal("gui.de.energySaturation.txt"));
            if (core.maxEnergySaturation > 0) {
                text.add(
                        Utils.round((double) core.energySaturation / (double) core.maxEnergySaturation * 100D, 100D)
                                + "%");
            }
            text.add(formatNumber(core.energySaturation) + " / " + formatNumber(core.maxEnergySaturation));
            drawHoveringText(text, mouseX, mouseY, fontRendererObj);
        } else if (GuiHelper.isInRect(221, 4, 18, 114, mouseX - guiLeft, mouseY - guiTop)) {
            text.add(StatCollector.translateToLocal("gui.de.fuelConversion.txt"));
            if (core.reactorFuel + core.convertedFuel > 0) {
                text.add(
                        Utils.round(
                                ((double) core.convertedFuel + core.conversionUnit)
                                        / ((double) core.convertedFuel + (double) core.reactorFuel)
                                        * 100D,
                                100D) + "%");
            }
            text.add(core.convertedFuel + " / " + (core.convertedFuel + core.reactorFuel));
            text.add(
                    "Full: " + (Utils.round(
                            (float) (core.convertedFuel + core.reactorFuel) / ConfigHandler.reactorFuelStorage * 100,
                            1000)) + "%");
            drawHoveringText(text, mouseX, mouseY, fontRendererObj);
        }

        if (showStatistics) {
            if (GuiHelper.isInRect(53, 15, 140, 18, mouseX - guiLeft, mouseY - guiTop)) {
                text.addAll(
                        fontRendererObj.listFormattedStringToWidth(
                                StatCollector.translateToLocal("gui.de.reacTempLoadFactor.txt"),
                                200));
                drawHoveringText(text, mouseX, mouseY, fontRendererObj);
            } else if (GuiHelper.isInRect(53, 40, 140, 18, mouseX - guiLeft, mouseY - guiTop)) {
                text.addAll(
                        fontRendererObj.listFormattedStringToWidth(
                                StatCollector.translateToLocal("gui.de.reacCoreMass.txt"),
                                200));
                drawHoveringText(text, mouseX, mouseY, fontRendererObj);
            } else if (GuiHelper.isInRect(53, 65, 140, 18, mouseX - guiLeft, mouseY - guiTop)) {
                text.addAll(
                        fontRendererObj.listFormattedStringToWidth(
                                StatCollector.translateToLocal("gui.de.reacGenRate.txt"),
                                200));
                drawHoveringText(text, mouseX, mouseY, fontRendererObj);
            } else if (GuiHelper.isInRect(53, 88, 140, 18, mouseX - guiLeft, mouseY - guiTop)) {
                text.addAll(
                        fontRendererObj.listFormattedStringToWidth(
                                StatCollector.translateToLocal("gui.de.reacInputRate.txt"),
                                200));
                drawHoveringText(text, mouseX, mouseY, fontRendererObj);
            } else if (GuiHelper.isInRect(53, 113, 140, 18, mouseX - guiLeft, mouseY - guiTop)) {
                text.addAll(
                        fontRendererObj.listFormattedStringToWidth(
                                StatCollector.translateToLocal("gui.de.reacConversionRate.txt"),
                                200));
                drawHoveringText(text, mouseX, mouseY, fontRendererObj);
            }
        }
    }

    private void drawStatistics() {
        double inputRate = core.fieldDrain / (1D - (core.fieldCharge / core.maxFieldCharge));
        fontRendererObj.drawString(StatCollector.translateToLocal("gui.de.tempLoad.name"), 55, 16, 0x0000FF);
        fontRendererObj.drawString(Utils.round(core.tempDrainFactor * 100D, 1D) + "%", 60, 2 + 24, 0);
        fontRendererObj.drawString(StatCollector.translateToLocal("gui.de.mass.name"), 55, 16 + 24, 0x0000FF);
        fontRendererObj.drawString(
                Utils.round((core.reactorFuel + core.convertedFuel) / 1296D, 100) + "m^3",
                60,
                2 + 2 * 24,
                0);
        fontRendererObj.drawString(StatCollector.translateToLocal("gui.de.genRate.name"), 55, 16 + 2 * 24, 0x0000FF);
        fontRendererObj.drawString(formatNumber((int) core.generationRate) + "RF/t", 60, 2 + 3 * 24, 0);
        fontRendererObj
                .drawString(StatCollector.translateToLocal("gui.de.fieldInputRate.name"), 55, 16 + 3 * 24, 0x0000FF);
        fontRendererObj
                .drawString(formatNumber((int) Math.min(inputRate, Integer.MAX_VALUE)) + "RF/t", 60, 2 + 4 * 24, 0);
        fontRendererObj
                .drawString(StatCollector.translateToLocal("gui.de.fuelConversion.name"), 55, 16 + 4 * 24, 0x0000FF);
        fontRendererObj
                .drawString(formatNumber((int) Math.round(core.fuelUseRate * 1000000D)) + "nb/t", 60, 2 + 5 * 24, 0);
    }

    private void drawStatus() {
        String status = StatCollector.translateToLocal("gui.de.status.txt") + ": ";
        switch (core.reactorState) {
            case OFFLINE -> status += EnumChatFormatting.DARK_GRAY;
            case STARTING, STOPPING -> status += EnumChatFormatting.RED;
            case ONLINE -> status += EnumChatFormatting.DARK_GREEN;
            case INVALID -> status += EnumChatFormatting.DARK_RED;
        }
        status += core.reactorState.toLocalizedString(core.canStart());
        fontRendererObj.drawString(status, xSize - 5 - fontRendererObj.getStringWidth(status), 125, 0);
    }

    @Override
    public void updateScreen() {
        collection.getComponent("DEACTIVATE")
                .setEnabled(core.reactorState == ReactorState.STARTING || core.reactorState == ReactorState.ONLINE);
        collection.getComponent("CHARGE").setEnabled(
                (core.reactorState == ReactorState.OFFLINE
                        || (core.reactorState == ReactorState.STOPPING && !core.canStart())) && core.canCharge());
        collection.getComponent("ACTIVATE").setEnabled(
                (core.reactorState == ReactorState.STARTING || core.reactorState == ReactorState.STOPPING)
                        && core.canStart());
        super.updateScreen();
    }

    @Override
    public void buttonClicked(int id, int button) {
        super.buttonClicked(id, button);
        if (id < 3) {
            container.sendObjectToServer(null, 20, id);
        } else if (id == 3) {
            showStatistics = !showStatistics;
            ((ComponentButton) collection.getComponent("STATS")).hoverText = showStatistics
                    ? StatCollector.translateToLocal("button.de.statsHide.txt")
                    : StatCollector.translateToLocal("button.de.statsShow.txt");
        }
    }
}
