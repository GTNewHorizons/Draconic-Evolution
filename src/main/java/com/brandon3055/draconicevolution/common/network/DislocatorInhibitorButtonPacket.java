package com.brandon3055.draconicevolution.common.network;

import com.brandon3055.draconicevolution.common.container.ContainerDislocatorInhibitor;
import com.brandon3055.draconicevolution.common.tileentities.TileDislocatorInhibitor;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class DislocatorInhibitorButtonPacket implements IMessage {

    private short index = 0;
    private short value = 0;

    public DislocatorInhibitorButtonPacket() {}

    public DislocatorInhibitorButtonPacket(byte index, short value) {
        this.index = index;
        this.value = value;
    }

    @Override
    public void toBytes(ByteBuf bytes) {
        bytes.writeByte(index);
        bytes.writeByte(value);
    }

    @Override
    public void fromBytes(ByteBuf bytes) {
        index = bytes.readByte();
        value = bytes.readByte();
    }

    public static class Handler implements IMessageHandler<DislocatorInhibitorButtonPacket, IMessage> {

        @Override
        public IMessage onMessage(DislocatorInhibitorButtonPacket message, MessageContext ctx) {
            if (!(ctx
                    .getServerHandler().playerEntity.openContainer instanceof ContainerDislocatorInhibitor container)) {
                return null;
            }
            TileDislocatorInhibitor tile = container.getTileEntity();
            if (tile == null) {
                return null;
            }

            switch (message.index) {
                case 0 -> tile.setRange(message.value);
                case 1 -> tile.setActivityControl(message.value);
                case 2 -> tile.setWhitelist(message.value == 1);
            }
            return null;
        }
    }
}
