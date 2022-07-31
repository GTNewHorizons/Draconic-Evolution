package com.brandon3055.draconicevolution.common.network;

import com.brandon3055.draconicevolution.common.items.tools.Magnet;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import java.util.Arrays;
import java.util.Optional;

public final class MagnetTogglePacket implements IMessage {

    @Override
    public void fromBytes(ByteBuf buf) {
        // do nothing
    }

    @Override
    public void toBytes(ByteBuf buf) {
        // do nothing
    }


    public static final class Handler implements IMessageHandler<MagnetTogglePacket, IMessage> {

        @Override
        public IMessage onMessage(MagnetTogglePacket message, MessageContext ctx) {
            EntityPlayer player = ctx.getServerHandler().playerEntity;

            Optional<ItemStack> magnet = Arrays.stream(player.inventory.mainInventory)
                    .filter(itemStack -> itemStack != null && itemStack.getItem() instanceof Magnet)
                    .findAny();

            magnet.ifPresent(Magnet::toggle);

            return null;
        }
    }
}
