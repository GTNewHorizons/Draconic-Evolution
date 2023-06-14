package com.brandon3055.draconicevolution.client.gui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.apache.commons.lang3.StringEscapeUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.common.lib.References;
import com.brandon3055.draconicevolution.common.network.ParticleGenPacket;
import com.brandon3055.draconicevolution.common.tileentities.TileParticleGenerator;

public class GUIParticleGenerator extends GuiScreen {

    private interface IProperty {

        void increaseValue(int amount);

        void decreaseValue(int amount);

        short getSyncValue();

        void createButtons(List<GuiButton> buttons, int x, int y, int propertyId);

        void updateButtons(int page);

        void drawLabel(FontRenderer fontRenderer, int x, int y);
    }

    private static class IntegerProperty implements IProperty {

        private final int page;
        private final String label;
        private final int minimumValue;
        private final int maximumValue;
        private int value;
        private GuiButton increaseButton;
        private GuiButton decreaseButton;

        public IntegerProperty(int page, String label, int value, int minimumValue, int maximumValue) {
            this.page = page;
            this.label = StatCollector.translateToLocal("gui.de.particleGenerator." + label);
            this.value = value;
            this.minimumValue = minimumValue;
            this.maximumValue = maximumValue;
        }

        @Override
        public void increaseValue(int amount) {
            value = Math.min(value + amount, maximumValue);
        }

        @Override
        public void decreaseValue(int amount) {
            value = Math.max(value - amount, minimumValue);
        }

        @Override
        public short getSyncValue() {
            return (short) value;
        }

        @Override
        public void createButtons(List<GuiButton> buttons, int x, int y, int propertyId) {
            buttons.add(increaseButton = new GuiButton(propertyId * 2, x, y, 20, 20, "+"));
            buttons.add(decreaseButton = new GuiButton(propertyId * 2 + 1, x + 71, y, 20, 20, "-"));
        }

        @Override
        public void updateButtons(int page) {
            increaseButton.visible = this.page == page;
            decreaseButton.visible = this.page == page;
        }

        @Override
        public void drawLabel(FontRenderer fontRenderer, int x, int y) {
            fontRenderer.drawString(label, x, y, 0x000000);
            fontRenderer.drawString(String.valueOf(value), x, y + 10, 0x000000);
        }
    }

    private static class FloatProperty implements IProperty {

        private final int page;
        private final String label;
        private final float minimumValue;
        private final float maximumValue;
        private final float scale;
        private float value;
        private GuiButton increaseButton;
        private GuiButton decreaseButton;

        public FloatProperty(int page, String label, float value, float minimumValue, float maximumValue, float scale) {
            this.page = page;
            this.label = StatCollector.translateToLocal("gui.de.particleGenerator." + label);
            this.value = value;
            this.minimumValue = minimumValue;
            this.maximumValue = maximumValue;
            this.scale = scale;
        }

        @Override
        public void increaseValue(int amount) {
            value = Math.min(value + (float) amount / scale, maximumValue);
        }

        @Override
        public void decreaseValue(int amount) {
            value = Math.max(value - (float) amount / scale, minimumValue);
        }

        @Override
        public short getSyncValue() {
            return (short) (value * scale);
        }

        @Override
        public void createButtons(List<GuiButton> buttons, int x, int y, int propertyId) {
            buttons.add(increaseButton = new GuiButton(propertyId * 2, x, y, 20, 20, "+"));
            buttons.add(decreaseButton = new GuiButton(propertyId * 2 + 1, x + 71, y, 20, 20, "-"));
        }

        @Override
        public void updateButtons(int page) {
            increaseButton.visible = this.page == page;
            decreaseButton.visible = this.page == page;
        }

        @Override
        public void drawLabel(FontRenderer fontRenderer, int x, int y) {
            float roundedValue = Math.round(value * scale) / scale;
            fontRenderer.drawString(label, x, y, 0x000000);
            fontRenderer.drawString(String.valueOf(roundedValue), x, y + 10, 0x000000);
        }
    }

    private final int xSize = 212;
    private final int ySize = 198;
    private final ResourceLocation guiTexture = new ResourceLocation(
            References.MODID.toLowerCase(),
            "textures/gui/ParticleGenerator.png");
    private int page = 1;
    private int infoPage = 0;

    private boolean canParticleCollide = false;
    private int selectedParticle = 1;
    private boolean isParticlesEnabled = true;

    private boolean isBeamEnabled = false;
    private boolean shouldRenderCore = false;

    private final TileParticleGenerator particleGenerator;
    private final Map<Integer, IProperty> properties;
    private GuiButton previousPage;
    private GuiButton nextPage;
    private GuiButton previousInfoPage;
    private GuiButton nextInfoPage;
    private GuiButton showInfo;
    private GuiButton hideInfo;
    private GuiButton collisionToggle;
    private GuiButton particleToggle;
    private GuiButton particleSelector;
    private GuiButton beamToggle;
    private GuiButton coreRenderToggle;
    private GuiButton settingsSaver;

    public GUIParticleGenerator(TileParticleGenerator particleGenerator) {
        super();
        this.particleGenerator = particleGenerator;
        this.properties = new HashMap<>();
        syncWithServer();
        updateScreen();
    }

    private static final String[] infoText = {
            StringEscapeUtils.unescapeJava(StatCollector.translateToLocal("gui.de.particleGenerator.info.1")),
            StringEscapeUtils.unescapeJava(StatCollector.translateToLocal("gui.de.particleGenerator.info.2")),
            StringEscapeUtils.unescapeJava(StatCollector.translateToLocal("gui.de.particleGenerator.info.3")),
            StringEscapeUtils.unescapeJava(StatCollector.translateToLocal("gui.de.particleGenerator.info.4")),
            EnumChatFormatting.DARK_RED
                    + StringEscapeUtils
                            .unescapeJava(StatCollector.translateToLocal("gui.de.particleGenerator.info.5.title"))
                    + EnumChatFormatting.BLACK
                    + StringEscapeUtils.unescapeJava(StatCollector.translateToLocal("gui.de.particleGenerator.info.5")),
            EnumChatFormatting.DARK_RED
                    + StringEscapeUtils
                            .unescapeJava(StatCollector.translateToLocal("gui.de.particleGenerator.info.6.title"))
                    + EnumChatFormatting.BLACK
                    + StringEscapeUtils.unescapeJava(StatCollector.translateToLocal("gui.de.particleGenerator.info.6")),
            StringEscapeUtils.escapeJava(StatCollector.translateToLocal("gui.de.particleGenerator.info.7")) };

    @Override
    public void drawScreen(int x, int y, float partialTicks) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        Minecraft.getMinecraft().renderEngine.bindTexture(guiTexture);
        int posX = (this.width - xSize) / 2;
        int posY = (this.height - ySize) / 2;
        drawTexturedModalRect(posX, posY, 0, 0, xSize, ySize);

        if (page == 1) {
            fontRendererObj.drawStringWithShadow(
                    StatCollector.translateToLocal("gui.de.particleGenerator.main.title"),
                    posX + 60,
                    posY + 5,
                    0x00FFFF);
            for (int column = 0; column < 2; column++) {
                for (int row = 0; row < 8; row++) {
                    IProperty property = properties.get(row + column * 10);
                    property.drawLabel(fontRendererObj, posX + 30 + column * 111, posY + 20 + row * 22);
                }
            }
            for (int row = 0; row < 8; row++) {
                fontRendererObj.drawStringWithShadow(" + ", posX + 98, posY + 25 + row * 22, 0xFFFFFF);
            }
        }
        if (page == 2) {
            fontRendererObj.drawStringWithShadow(
                    StatCollector.translateToLocal("gui.de.particleGenerator.main.title"),
                    posX + 60,
                    posY + 5,
                    0x00FFFF);
            for (int row = 0; row < 6; row++) {
                IProperty property = properties.get(row + 20);
                property.drawLabel(fontRendererObj, posX + 30, posY + 20 + row * 22);
            }
            for (int row = 0; row < 3; row++) {
                IProperty property = properties.get(row + 30);
                property.drawLabel(fontRendererObj, posX + 141, posY + 20 + row * 22);
                fontRendererObj.drawStringWithShadow(" + ", posX + 98, posY + 25 + row * 22, 0xFFFFFF);
            }
        }
        if (page == 3) {
            fontRendererObj.drawStringWithShadow(
                    StatCollector.translateToLocal("gui.de.particleGenerator.beam.title"),
                    posX + 65,
                    posY + 5,
                    0x00FFFF);
            for (int row = 0; row < 8; row++) {
                IProperty property = properties.get(row + 40);
                property.drawLabel(fontRendererObj, posX + 30, posY + 20 + row * 22);
            }
        }
        if (page == 10) {
            fontRendererObj.drawStringWithShadow(
                    StatCollector.translateToLocal("gui.de.particleGenerator.info.title"),
                    posX + 75,
                    posY + 5,
                    0x00FFFF);
            fontRendererObj.drawSplitString(infoText[infoPage], posX + 5, posY + 20, 200, 0x000000);
            fontRendererObj.drawSplitString(
                    StatCollector.translateToLocalFormatted("gui.de.particleGenerator.page.name", infoPage + 1),
                    posX + 88,
                    posY + 180,
                    200,
                    0xFF0000);
        }

        fontRendererObj.drawStringWithShadow(
                StatCollector.translateToLocal("gui.de.particleGenerator.hold.name"),
                posX + 215,
                posY + 11,
                0xFFFFFF);
        fontRendererObj.drawStringWithShadow("Shift: +-10", posX + 215, posY + 21, 0xFFFFFF);
        fontRendererObj.drawStringWithShadow("Ctrl: +-50", posX + 215, posY + 31, 0xFFFFFF);
        fontRendererObj.drawStringWithShadow("Shift+Ctrl: +-100", posX + 215, posY + 41, 0xFFFFFF);

        super.drawScreen(x, y, partialTicks);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void initGui() {
        int posX = (this.width - xSize) / 2;
        int posY = (this.height - ySize) / 2;
        buttonList.clear();

        // Navigation
        buttonList.add(previousPage = new GuiButton(100, posX - 20, posY + 177, 20, 20, "<"));
        buttonList.add(nextPage = new GuiButton(101, posX + 213, posY + 177, 20, 20, ">"));
        buttonList.add(
                showInfo = new GuiButton(
                        102,
                        posX - 21,
                        posY + 3,
                        20,
                        20,
                        StatCollector.translateToLocal("gui.de.particleGenerator.info.name")));
        buttonList.add(
                hideInfo = new GuiButton(
                        103,
                        posX - 31,
                        posY + 23,
                        30,
                        20,
                        StatCollector.translateToLocal("gui.de.particleGenerator.back.name")));
        buttonList.add(previousInfoPage = new GuiButton(104, posX + 4, posY + 174, 30, 20, "<-"));
        buttonList.add(nextInfoPage = new GuiButton(105, posX + 178, posY + 174, 30, 20, "->"));

        // First page
        for (int column = 0; column < 2; column++) {
            for (int row = 0; row < 8; row++) {
                int propertyId = row + column * 10;
                IProperty property = properties.get(propertyId);
                property.createButtons(buttonList, posX + 5 + column * 111, posY + 19 + row * 22, propertyId);
            }
        }

        // Second page
        for (int row = 0; row < 6; row++) {
            int propertyId = row + 20;
            IProperty property = properties.get(propertyId);
            property.createButtons(buttonList, posX + 5, posY + 19 + row * 22, propertyId);
        }
        for (int row = 0; row < 3; row++) {
            int propertyId = row + 30;
            IProperty property = properties.get(propertyId);
            property.createButtons(buttonList, posX + 116, posY + 19 + row * 22, propertyId);
        }
        buttonList.add(
                collisionToggle = new GuiButton(
                        110,
                        posX + 105,
                        posY + 19 + 3 * 22,
                        102,
                        20,
                        StatCollector.translateToLocal("gui.de.particleGenerator.blockCollision.name")));
        buttonList.add(
                particleSelector = new GuiButton(
                        111,
                        posX + 105,
                        posY + 19 + 4 * 22,
                        102,
                        20,
                        StatCollector.translateToLocal("gui.de.particleGenerator.particleSelected.name")));

        buttonList.add(
                particleToggle = new GuiButton(
                        112,
                        posX + 105,
                        posY + 19 + 5 * 22,
                        102,
                        20,
                        StatCollector.translateToLocal("gui.de.particleGenerator.enabled.name")));

        // Third page
        for (int row = 0; row < 8; row++) {
            int propertyId = row + 40;
            IProperty property = properties.get(propertyId);
            property.createButtons(buttonList, posX + 5, posY + 19 + row * 22, propertyId);
        }
        buttonList.add(
                beamToggle = new GuiButton(
                        120,
                        posX + 105,
                        posY + 19,
                        102,
                        20,
                        StatCollector.translateToLocal("gui.de.particleGenerator.enabled.name")));
        buttonList.add(
                coreRenderToggle = new GuiButton(
                        121,
                        posX + 105,
                        posY + 41,
                        102,
                        20,
                        StatCollector.translateToLocal("gui.de.particleGenerator.renderCore.name")));
        buttonList.add(
                settingsSaver = new GuiButton(
                        127,
                        posX + 105,
                        posY + 19 + 7 * 22,
                        102,
                        20,
                        StatCollector.translateToLocal("gui.de.particleGenerator.saveSettings.name")));

        updateButtons();
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id < 100) {
            int propertyId = button.id / 2;
            boolean shouldIncrease = button.id % 2 == 0;
            modifyProperty(propertyId, shouldIncrease);
            return;
        }
        short packetData = 0;
        switch (button.id) {
            case 100 -> {
                page = Math.max(page - 1, 1);
                packetData = (short) page;
            }
            case 101 -> {
                page = Math.min(page + 1, 3);
                packetData = (short) page;
            }
            case 102 -> {
                page = 10;
                packetData = (short) page;
            }
            case 103 -> {
                page = 1;
                packetData = (short) page;
            }
            case 104 -> infoPage = Math.max(infoPage - 1, 0);
            case 105 -> infoPage = Math.min(infoPage + 1, infoText.length - 1);
            case 110 -> {
                canParticleCollide = !canParticleCollide;
                packetData = (short) (canParticleCollide ? 1 : 0);
            }
            case 111 -> {
                selectedParticle = selectedParticle < TileParticleGenerator.MAXIMUM_PARTICLE_INDEX
                        ? selectedParticle + 1
                        : 1;
                packetData = (short) selectedParticle;
            }
            case 112 -> {
                isParticlesEnabled = !isParticlesEnabled;
                packetData = (short) (isParticlesEnabled ? 1 : 0);
            }
            case 120 -> {
                isBeamEnabled = !isBeamEnabled;
                packetData = (short) (isBeamEnabled ? 1 : 0);
            }
            case 121 -> {
                shouldRenderCore = !shouldRenderCore;
                packetData = (short) (shouldRenderCore ? 1 : 0);
            }
        }
        updateButtons();
        DraconicEvolution.network.sendToServer(
                new ParticleGenPacket(
                        (byte) button.id,
                        packetData,
                        particleGenerator.xCoord,
                        particleGenerator.yCoord,
                        particleGenerator.zCoord));
    }

    private void modifyProperty(int propertyId, boolean shouldIncrease) {
        boolean isShiftPressed = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        boolean isControlPressed = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)
                || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
        int amount = 1;
        if (isShiftPressed) {
            amount = 10;
        }
        if (isControlPressed) {
            amount = 50;
        }
        if (isShiftPressed && isControlPressed) {
            amount = 100;
        }
        IProperty property = properties.get(propertyId);
        if (shouldIncrease) {
            property.increaseValue(amount);
        } else {
            property.decreaseValue(amount);
        }
        DraconicEvolution.network.sendToServer(
                new ParticleGenPacket(
                        (byte) propertyId,
                        property.getSyncValue(),
                        particleGenerator.xCoord,
                        particleGenerator.yCoord,
                        particleGenerator.zCoord));
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private void syncWithServer() {
        properties.put(0, new IntegerProperty(1, "red.name", particleGenerator.red, 0, 255));
        properties.put(1, new IntegerProperty(1, "green.name", particleGenerator.green, 0, 255));
        properties.put(2, new IntegerProperty(1, "blue.name", particleGenerator.blue, 0, 255));
        properties.put(3, new FloatProperty(1, "motionX.name", particleGenerator.motionX, -5F, 5F, 1000F));
        properties.put(4, new FloatProperty(1, "motionY.name", particleGenerator.motionY, -5F, 5F, 1000F));
        properties.put(5, new FloatProperty(1, "motionZ.name", particleGenerator.motionZ, -5F, 5F, 1000F));
        properties.put(6, new FloatProperty(1, "scale.name", particleGenerator.scale, 0.01F, 50F, 100F));
        properties.put(7, new IntegerProperty(1, "life.name", particleGenerator.life, 0, 1000));

        properties.put(10, new IntegerProperty(1, "random.name", particleGenerator.randomRed, 0, 255));
        properties.put(11, new IntegerProperty(1, "random.name", particleGenerator.randomGreen, 0, 255));
        properties.put(12, new IntegerProperty(1, "random.name", particleGenerator.randomBlue, 0, 255));
        properties.put(13, new FloatProperty(1, "random.name", particleGenerator.randomMotionX, -5F, 5F, 1000F));
        properties.put(14, new FloatProperty(1, "random.name", particleGenerator.randomMotionY, -5F, 5F, 1000F));
        properties.put(15, new FloatProperty(1, "random.name", particleGenerator.randomMotionZ, -5F, 5F, 1000F));
        properties.put(16, new FloatProperty(1, "random.name", particleGenerator.randomScale, 0F, 50F, 100F));
        properties.put(17, new IntegerProperty(1, "random.name", particleGenerator.randomLife, 0, 1000));

        properties.put(20, new FloatProperty(2, "spawnX.name", particleGenerator.spawnX, -50F, 50F, 10F));
        properties.put(21, new FloatProperty(2, "spawnY.name", particleGenerator.spawnY, -50F, 50F, 10F));
        properties.put(22, new FloatProperty(2, "spawnZ.name", particleGenerator.spawnZ, -50F, 50F, 10F));
        properties.put(23, new IntegerProperty(2, "delay.name", particleGenerator.spawnRate, 1, 200));
        properties.put(24, new IntegerProperty(2, "fade.name", particleGenerator.fade, 0, 100));
        properties.put(25, new FloatProperty(2, "gravity.name", particleGenerator.gravity, -5F, 5F, 1000F));

        properties.put(30, new FloatProperty(2, "random.name", particleGenerator.randomSpawnX, -50F, 50F, 10F));
        properties.put(31, new FloatProperty(2, "random.name", particleGenerator.randomSpawnY, -50F, 50F, 10F));
        properties.put(32, new FloatProperty(2, "random.name", particleGenerator.randomSpawnZ, -50F, 50F, 10F));

        page = particleGenerator.page;
        canParticleCollide = particleGenerator.canParticleCollide;
        selectedParticle = particleGenerator.selectedParticle;
        isParticlesEnabled = particleGenerator.isParticlesEnabled;

        properties.put(40, new IntegerProperty(3, "red.name", particleGenerator.beamRed, 0, 255));
        properties.put(41, new IntegerProperty(3, "green.name", particleGenerator.beamGreen, 0, 255));
        properties.put(42, new IntegerProperty(3, "blue.name", particleGenerator.beamBlue, 0, 255));
        properties.put(43, new FloatProperty(3, "pitch.name", particleGenerator.beamPitch, -180F, 180F, 10F));
        properties.put(44, new FloatProperty(3, "yaw.name", particleGenerator.beamYaw, -180F, 180F, 10F));
        properties.put(45, new FloatProperty(3, "rotation.name", particleGenerator.beamRotation, -1F, 1F, 100F));
        properties.put(46, new FloatProperty(3, "scale.name", particleGenerator.beamScale, 0F, 5F, 100F));
        properties.put(47, new FloatProperty(3, "length.name", particleGenerator.beamLength, 0F, 320F, 100F));

        shouldRenderCore = particleGenerator.shouldRenderCore;
        isBeamEnabled = particleGenerator.isBeamEnabled;
    }

    private void updateButtons() {
        for (IProperty property : properties.values()) {
            property.updateButtons(page);
        }

        previousPage.visible = page > 1 && page <= 3;
        nextPage.visible = page < 3;
        if (page == 10) {
            showInfo.visible = false;
            hideInfo.visible = true;
            previousInfoPage.visible = infoPage > 0;
            nextInfoPage.visible = infoPage < infoText.length - 1;
        } else {
            showInfo.visible = true;
            hideInfo.visible = false;
            previousInfoPage.visible = false;
            nextInfoPage.visible = false;
        }
        final String onLabel = StatCollector.translateToLocal("gui.de.on.txt");
        final String offLabel = StatCollector.translateToLocal("gui.de.off.txt");
        collisionToggle.visible = page == 2;
        collisionToggle.displayString = StatCollector.translateToLocalFormatted(
                "gui.de.particleGenerator.blockCollision.name",
                canParticleCollide ? onLabel : offLabel);
        particleSelector.visible = page == 2;
        particleSelector.displayString = StatCollector
                .translateToLocalFormatted("gui.de.particleGenerator.particleSelected.name", selectedParticle);
        particleToggle.visible = page == 2;
        particleToggle.displayString = StatCollector.translateToLocalFormatted(
                "gui.de.particleGenerator.enabled.name",
                isParticlesEnabled ? onLabel : offLabel);
        beamToggle.visible = page == 3;
        beamToggle.displayString = StatCollector
                .translateToLocalFormatted("gui.de.particleGenerator.enabled.name", isBeamEnabled ? onLabel : offLabel);
        coreRenderToggle.visible = page == 3;
        coreRenderToggle.displayString = StatCollector.translateToLocalFormatted(
                "gui.de.particleGenerator.renderCore.name",
                shouldRenderCore ? onLabel : offLabel);
        settingsSaver.visible = page == 3;
    }
}
