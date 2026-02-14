package com.brandon3055.draconicevolution.common.network;

import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import com.brandon3055.draconicevolution.client.handler.ClientEventHandler;
import com.brandon3055.draconicevolution.common.items.tools.Magnet;
import com.brandon3055.draconicevolution.common.utils.InventoryUtils;
import com.brandon3055.draconicevolution.integration.ModHelper;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

public final class MagnetToggleSelfPickupAckPacket implements IMessage {

    private short status;

    public MagnetToggleSelfPickupAckPacket() {}

    public MagnetToggleSelfPickupAckPacket(short status) {
        this.status = status;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.status = buf.readShort();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeShort(this.status);
    }

    public static final class Handler implements IMessageHandler<MagnetToggleSelfPickupAckPacket, IMessage> {

        @SideOnly(Side.CLIENT)
        @Override
        public IMessage onMessage(MagnetToggleSelfPickupAckPacket message, MessageContext ctx) {
            EntityPlayer player = Minecraft.getMinecraft().thePlayer;
            Optional<ItemStack> magnetOptional = InventoryUtils.getItemInAnyPlayerInventory(player, Magnet.class);

            if (magnetOptional.isPresent()) {
                ItemStack itemStack = magnetOptional.get();
                Magnet.setSelfPickupStatus(itemStack, message.status);
                ClientEventHandler.statusDisplayManager.startDrawing(itemStack.copy());
                if (ModHelper.isGTNHLibLoaded) Magnet.renderHUDSelfPickupStatusChange();
            }

            return null;
        }
    }
}
