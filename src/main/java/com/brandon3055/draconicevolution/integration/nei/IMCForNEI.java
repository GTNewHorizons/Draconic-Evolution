package com.brandon3055.draconicevolution.integration.nei;

import static com.brandon3055.draconicevolution.common.lib.References.MODID;
import static com.brandon3055.draconicevolution.common.lib.References.MODNAME;

import net.minecraft.nbt.NBTTagCompound;

import cpw.mods.fml.common.event.FMLInterModComms;

public class IMCForNEI {

    public static void IMCSender() {
        sendHandler(
                "com.brandon3055.draconicevolution.integration.nei.ReactorNEIHandler",
                "DraconicEvolution:reactorCore");
        sendCatalyst("draconicevolution.reactor", "DraconicEvolution:reactorCore");
    }

    private static void sendHandler(String name, String itemStack) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("handler", name);
        nbt.setString("modName", MODNAME);
        nbt.setString("modId", MODID);
        nbt.setBoolean("modRequired", true);
        nbt.setString("itemName", itemStack);
        nbt.setInteger("handlerHeight", 105);
        nbt.setInteger("maxRecipesPerPage", 1);
        FMLInterModComms.sendMessage("NotEnoughItems", "registerHandlerInfo", nbt);
    }

    private static void sendCatalyst(String name, String itemStack, int priority) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("handlerID", name);
        nbt.setString("itemName", itemStack);
        nbt.setInteger("priority", priority);
        FMLInterModComms.sendMessage("NotEnoughItems", "registerCatalystInfo", nbt);
    }

    private static void sendCatalyst(String name, String itemStack) {
        sendCatalyst(name, itemStack, 0);
    }
}
