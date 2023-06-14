package com.brandon3055.draconicevolution.common.network;

import com.brandon3055.draconicevolution.common.container.ContainerPlayerDetector;
import com.brandon3055.draconicevolution.common.tileentities.TilePlayerDetectorAdvanced;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class PlayerDetectorButtonPacket implements IMessage {

    private short index = 0;
    private short value = 0;

    public PlayerDetectorButtonPacket() {}

    public PlayerDetectorButtonPacket(byte index, short value) {
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

    public static class Handler implements IMessageHandler<PlayerDetectorButtonPacket, IMessage> {

        @Override
        public IMessage onMessage(PlayerDetectorButtonPacket message, MessageContext ctx) {
            if (!(ctx.getServerHandler().playerEntity.openContainer instanceof ContainerPlayerDetector container)) {
                return null;
            }
            TilePlayerDetectorAdvanced tile = container.getDetector();
            if (tile == null) {
                return null;
            }

            switch (message.index) {
                case 0 -> tile.range = message.value;
                case 1 -> tile.isInWhiteListMode = message.value == 1;
                case 2 -> tile.isOutputInverted = message.value == 1;
            }
            ctx.getServerHandler().playerEntity.worldObj.markBlockForUpdate(tile.xCoord, tile.yCoord, tile.zCoord);
            return null;
        }
    }
}
