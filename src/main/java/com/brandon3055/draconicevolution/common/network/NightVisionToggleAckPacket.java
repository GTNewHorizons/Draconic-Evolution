package com.brandon3055.draconicevolution.common.network;

import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.common.lib.References;
import com.brandon3055.draconicevolution.common.utils.IConfigurableItem;
import com.brandon3055.draconicevolution.common.utils.InventoryUtils;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class NightVisionToggleAckPacket implements IMessage {

    private boolean nightVisionEnabled;

    public NightVisionToggleAckPacket() {}

    public NightVisionToggleAckPacket(boolean b) {
        this.nightVisionEnabled = b;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.nightVisionEnabled = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(nightVisionEnabled);
    }

    public static final class Handler implements IMessageHandler<NightVisionToggleAckPacket, IMessage> {

        @Override
        public IMessage onMessage(NightVisionToggleAckPacket message, MessageContext ctx) {
            EntityPlayer player = Minecraft.getMinecraft().thePlayer;
            Optional<ItemStack> helm = InventoryUtils.getDEArmorInPlayerInventory(player, 0);

            if (helm.isPresent()) {
                ItemStack stack = helm.get();
                IConfigurableItem.ProfileHelper
                        .setBoolean(stack, References.ARMOR_NIGHT_VISION_ACTIVE, message.nightVisionEnabled);
                DraconicEvolution.clientProxy().displayItem(stack);
            }

            return null;
        }
    }
}
