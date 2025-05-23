package com.brandon3055.draconicevolution.common.network;

import net.minecraft.item.ItemStack;

import com.brandon3055.draconicevolution.common.container.ContainerDislocatorInhibitor;
import com.brandon3055.draconicevolution.common.tileentities.TileDislocatorInhibitor;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class SlotFakeClickPacket implements IMessage {

    private short slotIndex = 0;
    private ItemStack stack;

    public SlotFakeClickPacket() {}

    public SlotFakeClickPacket(byte slotIndex, ItemStack stack) {
        this.slotIndex = slotIndex;
        this.stack = stack;
    }

    @Override
    public void toBytes(ByteBuf bytes) {
        bytes.writeByte(slotIndex);
        ByteBufUtils.writeItemStack(bytes, stack);
    }

    @Override
    public void fromBytes(ByteBuf bytes) {
        slotIndex = bytes.readByte();
        stack = ByteBufUtils.readItemStack(bytes);
    }

    public static class Handler implements IMessageHandler<SlotFakeClickPacket, IMessage> {

        @Override
        public IMessage onMessage(SlotFakeClickPacket message, MessageContext ctx) {
            if (!(ctx
                    .getServerHandler().playerEntity.openContainer instanceof ContainerDislocatorInhibitor container)) {
                return null;
            }
            TileDislocatorInhibitor tile = container.getTileEntity();
            if (tile == null) {
                return null;
            }
            tile.setFilterItem(message.slotIndex, message.stack);
            return null;
        }
    }
}
