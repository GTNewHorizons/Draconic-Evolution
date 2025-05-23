package com.brandon3055.draconicevolution.client.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.common.container.ContainerDislocatorInhibitor;
import com.brandon3055.draconicevolution.common.inventory.SlotFakeItem;
import com.brandon3055.draconicevolution.common.lib.References;
import com.brandon3055.draconicevolution.common.network.DislocatorInhibitorButtonPacket;
import com.brandon3055.draconicevolution.common.network.SlotFakeClickPacket;
import com.brandon3055.draconicevolution.common.tileentities.TileDislocatorInhibitor;

import codechicken.nei.VisiblityData;
import codechicken.nei.api.INEIGuiHandler;
import codechicken.nei.api.TaggedInventoryArea;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
@Optional.Interface(iface = "codechicken.nei.api.INEIGuiHandler", modid = "NotEnoughItems")
public class GUIDislocatorInhibitor extends GuiContainer implements INEIGuiHandler {

    private static final ResourceLocation texture = new ResourceLocation(
            References.MODID.toLowerCase(),
            "textures/gui/Inhibitor.png");
    private int range = 5;
    private TileDislocatorInhibitor.ActivityControlType activityControlType = TileDislocatorInhibitor.ActivityControlType.ALWAYS_ACTIVE;
    private boolean whitelist = false;

    private final ContainerDislocatorInhibitor container;

    public GUIDislocatorInhibitor(InventoryPlayer playerInventory, TileDislocatorInhibitor inhibitor) {
        super(inhibitor.getGuiContainer(playerInventory));
        this.container = (ContainerDislocatorInhibitor) inventorySlots;

        xSize = 176;
        ySize = 150;

        syncWithServer();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void initGui() {
        super.initGui();
        buttonList.clear();
        buttonList.add(new GuiButtonAHeight(0, guiLeft + 7, guiTop + 19, 18, 18, "-"));
        buttonList.add(new GuiButtonAHeight(1, guiLeft + 97, guiTop + 19, 18, 18, "+"));
        buttonList.add(new GuiButtonAHeight(2, guiLeft + 151, guiTop + 19, 18, 18, ""));
        buttonList.add(new GuiButtonAHeight(3, guiLeft + 151, guiTop + 41, 18, 18, ""));
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int x, int y) {
        fontRendererObj.drawString(
                StatCollector.translateToLocal("tile.draconicevolution:dislocatorInhibitor.name"),
                7,
                5,
                0x222222);
        drawGuiText();
        drawGreyOutSlots();
        drawButtonIcons();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {

        GL11.glColor4f(1, 1, 1, 1);

        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
    }

    @Override
    public void drawScreen(int x, int y, float partialTicks) {
        super.drawScreen(x, y, partialTicks);
        ArrayList<String> lines = new ArrayList<>();
        if (isButtonHovered(x, y, 7, 19, 18, 18)) {
            lines.add(StatCollector.translateToLocal("gui.de.dislocatorInhibitor.tooltip.range.decrease.txt"));
        } else if (isButtonHovered(x, y, 97, 19, 18, 18)) {
            lines.add(StatCollector.translateToLocal("gui.de.dislocatorInhibitor.tooltip.range.increase.txt"));
        } else if (isButtonHovered(x, y, 151, 19, 18, 18)) {
            switch (activityControlType) {
                case ALWAYS_ACTIVE -> lines
                        .add(StatCollector.translateToLocal("gui.de.dislocatorInhibitor.tooltip.activity.always.txt"));
                case WITH_REDSTONE -> lines
                        .add(StatCollector.translateToLocal("gui.de.dislocatorInhibitor.tooltip.activity.with.txt"));
                case WITHOUT_REDSTONE -> lines
                        .add(StatCollector.translateToLocal("gui.de.dislocatorInhibitor.tooltip.activity.without.txt"));
                case NEVER_ACTIVE -> lines
                        .add(StatCollector.translateToLocal("gui.de.dislocatorInhibitor.tooltip.activity.never.txt"));
            }
            lines.add(StatCollector.translateToLocal("gui.de.dislocatorInhibitor.tooltip.activity.cycle.txt"));
        } else if (isButtonHovered(x, y, 151, 41, 18, 18)) {
            if (whitelist) {
                lines.add(StatCollector.translateToLocal("gui.de.dislocatorInhibitor.tooltip.whitelist.txt"));
            } else {
                lines.add(StatCollector.translateToLocal("gui.de.dislocatorInhibitor.tooltip.blacklist.txt"));
            }
        }
        if (!lines.isEmpty()) {
            drawHoveringText(lines, x, y, fontRendererObj);
        }
    }

    private boolean isButtonHovered(int mouseX, int mouseY, int buttonX, int buttonY, int width, int height) {
        return (mouseX >= buttonX + guiLeft && mouseX < buttonX + width + guiLeft)
                && (mouseY >= buttonY + guiTop && mouseY < buttonY + height + guiTop);
    }

    @Override
    protected void handleMouseClick(Slot slotIn, int slotId, int clickedButton, int clickType) {
        if (slotIn instanceof SlotFakeItem sfi) {
            DraconicEvolution.network.sendToServer(
                    new SlotFakeClickPacket((byte) sfi.getSlotIndex(), this.mc.thePlayer.inventory.getItemStack()));
        } else {
            super.handleMouseClick(slotIn, slotId, clickedButton, clickType);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 0 -> { // Range -
                range = Math.max(range - 1, TileDislocatorInhibitor.MINIMUM_RANGE);
                DraconicEvolution.network.sendToServer(new DislocatorInhibitorButtonPacket((byte) 0, (byte) range));
            }
            case 1 -> { // Range +
                range = Math.min(range + 1, TileDislocatorInhibitor.MAXIMUM_RANGE);
                DraconicEvolution.network.sendToServer(new DislocatorInhibitorButtonPacket((byte) 0, (byte) range));
            }
            case 2 -> { // Cycle Activity control
                activityControlType = activityControlType.getNext();
                byte controlMode = (byte) (activityControlType.ordinal());
                DraconicEvolution.network.sendToServer(new DislocatorInhibitorButtonPacket((byte) 1, controlMode));
            }
            case 3 -> { // Toggle Whitelist/Blacklist mode
                whitelist = !whitelist;
                byte whitelistMode = (byte) (whitelist ? 1 : 0);
                DraconicEvolution.network.sendToServer(new DislocatorInhibitorButtonPacket((byte) 2, whitelistMode));
            }
        }
    }

    @Override
    public VisiblityData modifyVisiblity(GuiContainer gui, VisiblityData currentVisibility) {
        return currentVisibility;
    }

    @Override
    public Iterable<Integer> getItemSpawnSlots(GuiContainer gui, ItemStack item) {
        return null;
    }

    @Override
    public List<TaggedInventoryArea> getInventoryAreas(GuiContainer gui) {
        return Collections.emptyList();
    }

    @Override
    public boolean handleDragNDrop(GuiContainer gui, int mousex, int mousey, ItemStack draggedStack, int button) {
        if (gui instanceof GUIDislocatorInhibitor) {
            for (var slot : container.inventorySlots) {
                if (slot instanceof SlotFakeItem sfi) {
                    if (sfi.isOverSlot(mousex - this.guiLeft, mousey - this.guiTop)) {
                        draggedStack.stackSize = 0;
                        DraconicEvolution.network
                                .sendToServer(new SlotFakeClickPacket((byte) sfi.getSlotIndex(), draggedStack));
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean hideItemPanelSlot(GuiContainer gui, int x, int y, int w, int h) {
        return false;
    }

    private void drawGuiText() {
        fontRendererObj.drawString(
                StatCollector.translateToLocal("gui.de.dislocatorInhibitor.range.txt"),
                40,
                25,
                0x000000,
                false);
        fontRendererObj.drawString(String.valueOf(range), range < 10 ? 78 : 75, 25, 0x000000, false);
    }

    private void drawButtonIcons() {
        GL11.glColor4f(1, 1, 1, 1);

        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);

        // activity control button
        drawTexturedModalRect(151, 19, 0, 150 + (18 * activityControlType.ordinal()), 18, 18);

        // whitelist button
        drawTexturedModalRect(151, 41, 18, 150 + (whitelist ? 0 : 18), 18, 18);
    }

    private void drawGreyOutSlots() {

        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.5f);
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        for (int i = 0; i < 8; i++) {
            drawTexturedModalRect(7 + i * 18, 41, 36, 150, 18, 18);
        }
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_LIGHTING);
    }

    private void syncWithServer() {
        TileDislocatorInhibitor inhibitor = container.getTileEntity();
        whitelist = inhibitor.isWhitelist();
        range = inhibitor.getRange();
        activityControlType = inhibitor.getActivityControlType();
    }
}
