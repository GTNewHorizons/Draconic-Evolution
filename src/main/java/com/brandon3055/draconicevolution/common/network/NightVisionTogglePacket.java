package com.brandon3055.draconicevolution.common.network;

import java.util.Optional;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.common.lib.References;
import com.brandon3055.draconicevolution.common.utils.IConfigurableItem;
import com.brandon3055.draconicevolution.common.utils.InventoryUtils;
import com.brandon3055.draconicevolution.common.utils.ItemConfigField;

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
            if (helmetOptional.isEmpty()) return null;
            var helmet = helmetOptional.get();
            boolean hasNightVision = false;
            boolean defaultValue = false;
            // always true, but this way all checkers shut up about nullability & unchecked type
            if (helmet.getItem() instanceof IConfigurableItem asConfigurable) {
                // check from fields if the helmet supports night vision & grab it' default value
                var fields = asConfigurable.getFields(helmet, 0);
                for (ItemConfigField field : fields) {
                    if (References.ARMOR_NIGHT_VISION_ACTIVE.equals(field.name)
                            && field.datatype == References.BOOLEAN_ID) {
                        hasNightVision = true;
                        // should be a safe cast, because data type is checked above
                        defaultValue = (boolean) field.value;
                        break;
                    }
                }
            }
            if (!hasNightVision) return null;

            var inverted = !IConfigurableItem.ProfileHelper
                    .getBoolean(helmet, References.ARMOR_NIGHT_VISION_ACTIVE, defaultValue);
            IConfigurableItem.ProfileHelper.setBoolean(helmet, References.ARMOR_NIGHT_VISION_ACTIVE, inverted);
            DraconicEvolution.network.sendTo(new NightVisionToggleAckPacket(inverted), player);

            return null;
        }
    }
}
