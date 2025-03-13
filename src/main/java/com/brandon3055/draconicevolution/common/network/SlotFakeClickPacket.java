package com.brandon3055.draconicevolution.common.network;

import net.minecraft.item.ItemStack;

import com.brandon3055.draconicevolution.common.container.ContainerDislocatorInhibitor;
import com.brandon3055.draconicevolution.common.tileentities.TileDislocatorInhibitor;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class SlotFakeClickPacket implements IMessage {

    private short slotIndex = 0;

    public SlotFakeClickPacket() {}

    public SlotFakeClickPacket(byte slotIndex) {
        this.slotIndex = slotIndex;
    }

    @Override
    public void toBytes(ByteBuf bytes) {
        bytes.writeByte(slotIndex);
    }

    @Override
    public void fromBytes(ByteBuf bytes) {
        slotIndex = bytes.readByte();
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
            ItemStack currentItem = ctx.getServerHandler().playerEntity.inventory.getItemStack();
            tile.setFilterItem(message.slotIndex, currentItem);

            ctx.getServerHandler().playerEntity.worldObj.markBlockForUpdate(tile.xCoord, tile.yCoord, tile.zCoord);
            return null;
        }
    }
}
