package com.brandon3055.draconicevolution.client.keybinding;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.common.ModItems;
import com.brandon3055.draconicevolution.common.items.tools.Magnet;
import com.brandon3055.draconicevolution.common.items.tools.baseclasses.ToolHandler;
import com.brandon3055.draconicevolution.common.lib.References;
import com.brandon3055.draconicevolution.common.network.ButtonPacket;
import com.brandon3055.draconicevolution.common.network.MagnetTogglePacket;
import com.brandon3055.draconicevolution.common.network.MagnetToggleSelfPickupPacket;
import com.brandon3055.draconicevolution.common.network.PlacedItemPacket;
import com.brandon3055.draconicevolution.common.network.TeleporterPacket;
import com.brandon3055.draconicevolution.common.utils.IConfigurableItem;
import com.brandon3055.draconicevolution.common.utils.InventoryUtils;
import com.brandon3055.draconicevolution.common.utils.ItemNBTHelper;
import com.brandon3055.draconicevolution.integration.ModHelper;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Created by Brandon on 14/08/2014.
 */
@SideOnly(Side.CLIENT)
public class KeyInputHandler {

    private final KeyBinding placeItem;
    private final KeyBinding toolConfig;
    private final KeyBinding toolProfileChange;
    private final KeyBinding toggleFlight;
    private final KeyBinding toggleMagnet;
    private final KeyBinding toggleMagnetSelfPickup;

    public KeyInputHandler() {
        placeItem = new KeyBinding("key.placeItem", Keyboard.KEY_V, References.MODNAME);
        toolConfig = new KeyBinding("key.toolConfig", Keyboard.KEY_NONE, References.MODNAME);
        toolProfileChange = new KeyBinding("key.toolProfileChange", Keyboard.KEY_NONE, References.MODNAME);
        toggleFlight = new KeyBinding("key.toggleFlight", Keyboard.KEY_NONE, References.MODNAME);
        toggleMagnet = new KeyBinding("key.toggleMagnet", Keyboard.KEY_NONE, References.MODNAME);
        if (ModHelper.isHodgepodgeLoaded) {
            toggleMagnetSelfPickup = new KeyBinding(
                    "key.toggleMagnetSelfPickup",
                    Keyboard.KEY_NONE,
                    References.MODNAME);
        } else {
            toggleMagnetSelfPickup = null;
        }
        ClientRegistry.registerKeyBinding(placeItem);
        ClientRegistry.registerKeyBinding(toolConfig);
        ClientRegistry.registerKeyBinding(toolProfileChange);
        ClientRegistry.registerKeyBinding(toggleFlight);
        ClientRegistry.registerKeyBinding(toggleMagnet);
        if (toggleMagnetSelfPickup != null) {
            ClientRegistry.registerKeyBinding(toggleMagnetSelfPickup);
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;

        if (this.placeItem.isPressed()) {
            handlePlaceItemKey();
        } else if (this.toolConfig.isPressed()) {
            handleToolConfigKey();
        } else if (this.toolProfileChange.isPressed() && player != null && player.getItemInUse() == null) {
            handleToolProfileChangeKey(player);
        } else if (this.toggleFlight.isPressed() && player != null) {
            handleToggleFlightKey(player);
        } else if (this.toggleMagnet.isPressed()) {
            handleToggleMagnetKey(player);
        } else if (ModHelper.isHodgepodgeLoaded && this.toggleMagnetSelfPickup.isPressed()) {
            handleToggleMagnetSelfPickup(player);
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onMouseInput(InputEvent.MouseInputEvent event) {
        EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;

        if (this.placeItem.isPressed()) {
            handlePlaceItemKey();
        } else if (this.toolConfig.isPressed()) {
            handleToolConfigKey();
        } else if (this.toolProfileChange.isPressed() && player != null) {
            handleToolProfileChangeKey(player);
        } else if (this.toggleFlight.isPressed() && player != null) {
            handleToggleFlightKey(player);
        } else if (this.toggleMagnet.isPressed()) {
            handleToggleMagnetKey(player);
        } else if (ModHelper.isHodgepodgeLoaded && this.toggleMagnetSelfPickup.isPressed()) {
            handleToggleMagnetSelfPickup(player);
        }

        if (player != null) {
            int change = Mouse.getEventDWheel();
            if (change == 0 || !player.isSneaking()) return;

            if (change > 0) {
                ItemStack item = player.inventory.getStackInSlot(previousSlot(1, player.inventory.currentItem));
                if (item != null && Objects.equals(item.getItem(), ModItems.teleporterMKII)) {
                    player.inventory.currentItem = previousSlot(1, player.inventory.currentItem);
                    DraconicEvolution.network.sendToServer(new TeleporterPacket(TeleporterPacket.SCROLL, -1, false));
                }
            } else {
                ItemStack item = player.inventory.getStackInSlot(previousSlot(-1, player.inventory.currentItem));
                if (item != null && Objects.equals(item.getItem(), ModItems.teleporterMKII)) {
                    player.inventory.currentItem = previousSlot(-1, player.inventory.currentItem);
                    DraconicEvolution.network.sendToServer(new TeleporterPacket(TeleporterPacket.SCROLL, 1, false));
                }
            }
        }
    }

    private void handlePlaceItemKey() {
        EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
        WorldClient world = Minecraft.getMinecraft().theWorld;
        MovingObjectPosition mop = ToolHandler.raytraceFromEntity(world, player, 4.5D);
        if (mop != null) {
            DraconicEvolution.network
                    .sendToServer(new PlacedItemPacket((byte) mop.sideHit, mop.blockX, mop.blockY, mop.blockZ));
        }
    }

    private void handleToolConfigKey() {
        DraconicEvolution.network.sendToServer(new ButtonPacket(ButtonPacket.ID_TOOLCONFIG, false));
    }

    private void handleToolProfileChangeKey(@Nonnull EntityClientPlayerMP player) {
        DraconicEvolution.network.sendToServer(new ButtonPacket(ButtonPacket.ID_TOOL_PROFILE_CHANGE, false));

        ItemStack stack = player.getHeldItem();
        if (stack != null && stack.getItem() instanceof IConfigurableItem
                && ((IConfigurableItem) stack.getItem()).hasProfiles()) {
            int preset = ItemNBTHelper.getInteger(stack, "ConfigProfile", 0);
            if (++preset >= 5) preset = 0;
            ItemNBTHelper.setInteger(stack, "ConfigProfile", preset);
        }
    }

    private void handleToggleFlightKey(@Nonnull EntityClientPlayerMP player) {
        if (player.capabilities.allowFlying) {
            if (player.capabilities.isFlying) {
                player.capabilities.isFlying = false;
            } else {
                player.capabilities.isFlying = true;
                if (player.onGround) {
                    player.setPosition(player.posX, player.posY + 0.05D, player.posZ);
                    player.motionY = 0;
                }
            }
            player.sendPlayerAbilities();
        }
    }

    private void handleToggleMagnetKey(EntityClientPlayerMP player) {
        Optional<ItemStack> magnetOptional = InventoryUtils.getItemInAnyPlayerInventory(player, Magnet.class);

        if (magnetOptional.isPresent()) {
            DraconicEvolution.network.sendToServer(new MagnetTogglePacket());
        }
    }

    private void handleToggleMagnetSelfPickup(EntityClientPlayerMP player) {
        Optional<ItemStack> magnetOptional = InventoryUtils.getItemInAnyPlayerInventory(player, Magnet.class);

        if (magnetOptional.isPresent()) {
            DraconicEvolution.network.sendToServer(new MagnetToggleSelfPickupPacket());
        }
    }

    private int previousSlot(int i, int c) {
        if (c > 0 && c < 8) return c + i;
        if (c == 0 && i < 0) return 8;
        if (c == 8 && i > 0) return 0;
        if (c == 150 && i < 0) return 152;
        return c + i;
    }

    public int toolConfigKeycode() {
        return this.toolConfig.getKeyCode();
    }
}
