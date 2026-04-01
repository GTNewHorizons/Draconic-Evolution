package com.brandon3055.draconicevolution.common.network;

import java.util.Optional;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.common.items.tools.Magnet;
import com.brandon3055.draconicevolution.common.utils.InventoryUtils;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public final class MagnetToggleSelfPickupPacket implements IMessage {

    public MagnetToggleSelfPickupPacket() {}

    @Override
    public void fromBytes(ByteBuf buf) {
        // do nothing
    }

    @Override
    public void toBytes(ByteBuf buf) {
        // do nothing
    }

    public static final class Handler implements IMessageHandler<MagnetToggleSelfPickupPacket, IMessage> {

        @Override
        public IMessage onMessage(MagnetToggleSelfPickupPacket message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;

            Optional<ItemStack> magnetOptional = InventoryUtils.getItemInAnyPlayerInventory(player, Magnet.class);
            magnetOptional.ifPresent(itemStack -> {
                Magnet.toggleSelfPickupStatus(itemStack);
                DraconicEvolution.network.sendTo(
                        new MagnetToggleSelfPickupAckPacket(Magnet.getSelfPickupStatusShort(itemStack)),
                        player);
            });

            return null;
        }
    }
}
