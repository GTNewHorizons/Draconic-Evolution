package com.brandon3055.draconicevolution.common.network;

import java.util.Optional;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import com.brandon3055.draconicevolution.common.lib.References;
import com.brandon3055.draconicevolution.common.utils.IConfigurableItem;
import com.brandon3055.draconicevolution.common.utils.InventoryUtils;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class NightVisionTogglePacket implements IMessage {

    @Override
    public void fromBytes(ByteBuf buf) {
        // do nothing
    }

    @Override
    public void toBytes(ByteBuf buf) {
        // do nothing
    }

    public static final class Handler implements IMessageHandler<NightVisionTogglePacket, IMessage> {

        @Override
        public IMessage onMessage(NightVisionTogglePacket message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;

            Optional<ItemStack> helmetOptional = InventoryUtils.getDEArmorInPlayerInventory(player, 0);
            helmetOptional.ifPresent(itemStack -> {
                var old = IConfigurableItem.ProfileHelper
                        .getBoolean(itemStack, References.ARMOR_NIGHT_VISION_ACTIVE, false);
                IConfigurableItem.ProfileHelper.setBoolean(itemStack, References.ARMOR_NIGHT_VISION_ACTIVE, !old);
            });

            return null;
        }
    }
}
