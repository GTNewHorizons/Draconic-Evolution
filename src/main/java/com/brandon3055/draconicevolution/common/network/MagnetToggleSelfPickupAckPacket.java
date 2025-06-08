package com.brandon3055.draconicevolution.common.network;

import com.brandon3055.draconicevolution.client.handler.ClientEventHandler;
import com.brandon3055.draconicevolution.common.items.tools.Magnet;
import com.brandon3055.draconicevolution.common.utills.InventoryUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import java.util.Optional;

public final class MagnetToggleSelfPickupAckPacket implements IMessage {

    private int status;

    public MagnetToggleSelfPickupAckPacket() {}

    public MagnetToggleSelfPickupAckPacket(int status) {
        this.status = status;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.status = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.status);
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
            }

            return null;
        }
    }
}
