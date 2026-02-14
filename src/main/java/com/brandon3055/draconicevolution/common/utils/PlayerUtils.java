package com.brandon3055.draconicevolution.common.utils;

import net.minecraft.server.MinecraftServer;

import cpw.mods.fml.common.FMLCommonHandler;

public class PlayerUtils {

    public static boolean isOp(String paramString) {
        MinecraftServer localMinecraftServer = FMLCommonHandler.instance().getMinecraftServerInstance();
        paramString = paramString.trim();
        for (String str : localMinecraftServer.getConfigurationManager().func_152606_n()) {
            if (paramString.equalsIgnoreCase(str)) {
                return true;
            }
        }
        return false;
    }
}
