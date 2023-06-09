package com.brandon3055.draconicevolution.common.network;

import net.minecraft.inventory.Container;

import com.brandon3055.draconicevolution.common.container.ContainerPlayerDetector;
import com.brandon3055.draconicevolution.common.tileentities.TilePlayerDetectorAdvanced;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class PlayerDetectorStringPacket implements IMessage {

    private int index = 0;
    private String name = "";

    public PlayerDetectorStringPacket() {}

    public PlayerDetectorStringPacket(byte index, String name) {
        this.index = index;
        this.name = name;
    }

    @Override
    public void toBytes(ByteBuf bytes) {
        bytes.writeByte(index);
        ByteBufUtils.writeUTF8String(bytes, name);
    }

    @Override
    public void fromBytes(ByteBuf bytes) {
        index = bytes.readByte();
        name = ByteBufUtils.readUTF8String(bytes);
    }

    public static class Handler implements IMessageHandler<PlayerDetectorStringPacket, IMessage> {

        @Override
        public IMessage onMessage(PlayerDetectorStringPacket message, MessageContext ctx) {
            Container container = ctx.getServerHandler().playerEntity.openContainer;
            if (container instanceof ContainerPlayerDetector) {
                TilePlayerDetectorAdvanced detector = ((ContainerPlayerDetector) container).getDetector();
                if (detector != null) {
                    detector.names[message.index] = message.name;
                    ctx.getServerHandler().playerEntity.worldObj
                            .markBlockForUpdate(detector.xCoord, detector.yCoord, detector.zCoord);
                }
            }
            return null;
        }
    }
}
