package com.brandon3055.draconicevolution.common.utils.handlers;

import java.io.File;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/**
 * Created by Brandon on 7/6/2015.
 */
public class FileHandler {

    public static File configFolder;
    public static File mcDirectory;

    public static void init(FMLPreInitializationEvent event) {
        configFolder = event.getModConfigurationDirectory();
        mcDirectory = configFolder.getParentFile();
    }
}
