package com.brandon3055.draconicevolution.client.gui;

import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.common.container.ContainerDislocatorInhibitor;
import com.brandon3055.draconicevolution.common.inventory.SlotFakeItem;
import com.brandon3055.draconicevolution.common.lib.References;
import com.brandon3055.draconicevolution.common.network.DislocatorInhibitorButtonPacket;
import com.brandon3055.draconicevolution.common.network.SlotFakeClickPacket;
import com.brandon3055.draconicevolution.common.tileentities.TileDislocatorInhibitor;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;

@SideOnly(Side.CLIENT)
public class GUIDislocatorInhibitor extends GuiContainer {

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
        ySize = 142;

        syncWithServer();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void initGui() {
        super.initGui();
        buttonList.clear();
        buttonList.add(new GuiButtonAHeight(0, guiLeft + 7, guiTop + 11, 18, 18, "-"));
        buttonList.add(new GuiButtonAHeight(1, guiLeft + 97, guiTop + 11, 18, 18, "+"));
        buttonList.add(new GuiButtonAHeight(2, guiLeft + 151, guiTop + 11, 18, 18, activityControlType.name().substring(0, 1)));
        buttonList.add(new GuiButtonAHeight(3, guiLeft + 151, guiTop + 33, 18, 18, whitelist ? "W" : "B"));
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int x, int y) {
        drawGuiText();
        // todo draw grey outs for fake item slots
        // todo draw button icons
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
        if ((x - guiLeft > 7 && x - guiLeft < 25) && (y - guiTop > 11 && y - guiTop < 29)) {
            lines.add("Increase Range");
        } else if ((x - guiLeft > 97 && x - guiLeft < 115) && (y - guiTop > 11 && y - guiTop < 29)) {
            lines.add("Decrease Range");
        } else if ((x - guiLeft > 151 && x - guiLeft < 169) && (y - guiTop > 11 && y - guiTop < 29)) {
            switch (activityControlType) {
                case ALWAYS_ACTIVE -> lines.add("Active regardles of redstone power");
                case WITH_REDSTONE -> lines.add("Active with redstone power");
                case WITHOUT_REDSTONE -> lines.add("Active without redstone power");
                case NEVER_ACTIVE -> lines.add("Always inactive");
            }
            lines.add("Click to cycle through activity control");
        } else if ((x - guiLeft > 151 && x - guiLeft < 169) && (y - guiTop > 33 && y - guiTop < 51)) {
            if (whitelist) {
                lines.add("Only items in filter will be blocked");
            } else {
                lines.add("Items in filter will NOT be blocked");
            }
        }
        if (!lines.isEmpty()) {
            drawHoveringText(lines, x, y, fontRendererObj);
        }
    }

    @Override
    protected void handleMouseClick(Slot slotIn, int slotId, int clickedButton, int clickType) {
        if (slotIn instanceof SlotFakeItem sfi) {
            DraconicEvolution.network.sendToServer(new SlotFakeClickPacket((byte) sfi.getSlotIndex()));
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

    private void drawGuiText() {
        fontRendererObj.drawString("Range:", 40, 17, 0x000000, false);
        fontRendererObj.drawString(String.valueOf(range), range < 10 ? 78 : 75, 17, 0x000000, false);
    }

    private void syncWithServer() {
        TileDislocatorInhibitor inhibitor = container.getTileEntity();
        whitelist = inhibitor.isWhitelist();
        range = inhibitor.getRange();
        activityControlType = inhibitor.getActivityControlType();
    }
}
