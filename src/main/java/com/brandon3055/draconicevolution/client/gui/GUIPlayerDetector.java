package com.brandon3055.draconicevolution.client.gui;

import java.util.ArrayList;
import java.util.Arrays;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.common.container.ContainerPlayerDetector;
import com.brandon3055.draconicevolution.common.lib.References;
import com.brandon3055.draconicevolution.common.network.PlayerDetectorButtonPacket;
import com.brandon3055.draconicevolution.common.network.PlayerDetectorStringPacket;
import com.brandon3055.draconicevolution.common.tileentities.TilePlayerDetectorAdvanced;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GUIPlayerDetector extends GuiContainer {

    private static final ResourceLocation texture = new ResourceLocation(
            References.MODID.toLowerCase(),
            "textures/gui/PlayerDetector.png");

    private final ContainerPlayerDetector container;
    private boolean isInEditMode = false;
    private int range = 0;
    private boolean isInWhitelistMode = false;
    private boolean isOutputInverted = false;
    private boolean isInitScheduled = false;
    private int initTick = 0;
    private final String[] names = new String[42];
    private GuiTextField selectedNameText;
    private int selectedMameIndex = -1;

    public GUIPlayerDetector(InventoryPlayer playerInventory, TilePlayerDetectorAdvanced detector) {
        super(detector.getGuiContainer(playerInventory));
        container = (ContainerPlayerDetector) inventorySlots;
        Arrays.fill(names, "");

        xSize = 176;
        ySize = 198;

        syncWithServer();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int x, int y) {
        GL11.glColor4f(1, 1, 1, 1);

        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        if (isInEditMode) {
            drawTexturedModalRect(guiLeft + 3, guiTop + ySize / 2, 3, 3, xSize - 6, (ySize / 2) - 3);
            drawNameChart(x, y);
        }
        if (container.shouldShowInventory()) {
            drawTexturedModalRect(guiLeft + 142, guiTop + 19, xSize, 0, 23, 41);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int x, int y) {
        drawGuiText();
    }

    @Override
    public void drawScreen(int x, int y, float partialTicks) {
        super.drawScreen(x, y, partialTicks);
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Camouflage");
        if ((x - guiLeft > 142 && x - guiLeft < 160) && (y - guiTop > 19 && y - guiTop < 37)
                && container.shouldShowInventory()) {
            drawHoveringText(lines, x, y, fontRendererObj);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void initGui() {
        super.initGui();
        buttonList.clear();
        if (!isInEditMode) {
            String mode = isInWhitelistMode ? "White List" : "Black List";
            int center = width / 2;
            buttonList.add(new GuiButton(0, center - 20 - 20, guiTop + 20, 20, 20, "+"));
            buttonList.add(new GuiButton(1, center + 20, guiTop + 20, 20, 20, "-"));
            buttonList.add(new GuiButton(3, center - 40, guiTop + 45, 60, 20, mode));
            buttonList.add(new GuiButton(4, center + 20, guiTop + 45, 20, 20, "!"));
            buttonList.add(new GuiButton(6, center - 40, guiTop + 70, 80, 20, "Invert Output"));
        } else {
            buttonList.add(new GuiButton(5, guiLeft - 40, guiTop + ySize - 20, 40, 20, "Back"));
        }

        selectedNameText = new GuiTextField(fontRendererObj, 4, -12, 168, 12);
        selectedNameText.setTextColor(-1);
        selectedNameText.setDisabledTextColour(-1);
        selectedNameText.setEnableBackgroundDrawing(true);
        selectedNameText.setMaxStringLength(40);
        selectedNameText.setVisible(isInEditMode);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 0 -> { // Range +
                range = Math.min(range + 1, TilePlayerDetectorAdvanced.MAXIMUM_RANGE);
                DraconicEvolution.network.sendToServer(new PlayerDetectorButtonPacket((byte) 0, (byte) range));
            }
            case 1 -> { // Range -
                range = Math.max(range - 1, TilePlayerDetectorAdvanced.MINIMUM_RANGE);
                DraconicEvolution.network.sendToServer(new PlayerDetectorButtonPacket((byte) 0, (byte) range));
            }
            case 3 -> { // Edit Whitelist/Blacklist
                isInitScheduled = true;
                isInEditMode = true;
                container.setShouldShowInventory(false);
            }
            case 4 -> { // Toggle Whitelist/Blacklist mode
                isInitScheduled = true;
                isInWhitelistMode = !isInWhitelistMode;
                byte whitelistMode = (byte) (isInWhitelistMode ? 1 : 0);
                DraconicEvolution.network.sendToServer(new PlayerDetectorButtonPacket((byte) 1, whitelistMode));
            }
            case 5 -> { // Back
                isInitScheduled = true;
                isInEditMode = false;
                container.setShouldShowInventory(true);
            }
            case 6 -> { // Invert Output
                isInitScheduled = true;
                isOutputInverted = !isOutputInverted;
                byte outputMode = (byte) (isOutputInverted ? 1 : 0);
                DraconicEvolution.network.sendToServer(new PlayerDetectorButtonPacket((byte) 2, outputMode));
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (this.selectedNameText.textboxKeyTyped(typedChar, keyCode)) {
            return;
        }
        if (keyCode == 28) {
            if (selectedNameText.isFocused()) {
                names[selectedMameIndex] = selectedNameText.getText();
                selectedNameText.setText("");
                selectedNameText.setFocused(false);
                DraconicEvolution.network.sendToServer(
                        new PlayerDetectorStringPacket((byte) selectedMameIndex, names[selectedMameIndex]));
                selectedMameIndex = -1;
            }
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    public void updateScreen() {
        if (isInitScheduled) {
            initTick++;
        }
        if (initTick > 1) {
            initTick = 0;
            isInitScheduled = false;
            initGui();
        }
        super.updateScreen();
    }

    @Override
    protected void mouseClicked(int x, int y, int mouseButton) {
        super.mouseClicked(x, y, mouseButton);
        if (isInEditMode) {
            selectName(x - guiLeft, y - guiTop);
        }
    }

    private void drawGuiText() {
        if (isInEditMode) {
            if (selectedMameIndex != -1) {
                drawCenteredString(fontRendererObj, "Press Enter to save", xSize / 2, -22, 0xFF0000);
            }
            for (int row = 0; row < 21; row++) {
                for (int column = 0; column < 2; column++) {
                    if (row + column * 21 != selectedMameIndex) {
                        String name = names[row + column * 21];
                        if (name.length() > 13) {
                            name = name.substring(0, 13) + "...";
                        }
                        fontRendererObj.drawString(name, 5 + column * 84, 6 + row * 9, 0x980000);
                    }
                }
            }
        } else {
            drawCenteredString(fontRendererObj, "Advanced Player Detector", xSize / 2, 5, 0x00FFFF);

            fontRendererObj.drawString("Range:", 73, 21, 0x000000, false);
            fontRendererObj.drawString("Output Inverted: " + isOutputInverted, 33, 97, 0x000000, false);
            fontRendererObj.drawString(String.valueOf(range), range < 10 ? 85 : 82, 31, 0x000000, false);
        }
        selectedNameText.drawTextBox();
    }

    private void drawNameChart(int rawX, int rawY) {
        int x = rawX - guiLeft;
        int y = rawY - guiTop;

        for (int row = 0; row < 21; row++) {
            drawTexturedModalRect(guiLeft + 4, guiTop + 4 + row * 9, 0, ySize, 186, 10);
        }

        for (int row = 0; row < 21; row++) {
            for (int column = 0; column < 2; column++) {
                if ((x > 4 + column * 84 && x < (xSize / 2) - 1 + column * 82) && (y > 4 + row * 9 && y < 13 + row * 9)
                        || row + column * 21 == selectedMameIndex)
                    drawTexturedModalRect(guiLeft + 5 + column * 84, guiTop + 5 + row * 9, 0, ySize + 10, 82, 8);
            }
        }
    }

    public void selectName(int x, int y) {
        if (isInitScheduled) {
            return;
        }

        for (int row = 0; row < 21; row++) {
            for (int column = 0; column < 2; column++) {
                if ((x > 4 + column * 84 && x < (xSize / 2) - 1 + column * 82)
                        && (y > 4 + row * 9 && y < 13 + row * 9)) {
                    selectedMameIndex = row + column * 21;
                    selectedNameText.setText(names[row + column * 21]);
                    selectedNameText.setFocused(true);
                    return;
                }
            }
        }
    }

    private void syncWithServer() {
        TilePlayerDetectorAdvanced detector = container.getDetector();
        isInWhitelistMode = detector.isInWhiteListMode;
        for (int i = 0; i < detector.names.length; i++) {
            if (detector.names[i] != null) {
                names[i] = detector.names[i];
            }
        }
        range = detector.range;
        isOutputInverted = detector.isOutputInverted;
    }
}
