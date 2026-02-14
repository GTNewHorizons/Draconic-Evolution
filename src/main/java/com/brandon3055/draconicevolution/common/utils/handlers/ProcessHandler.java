package com.brandon3055.draconicevolution.common.utils.handlers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * This is a class used to run processes that implement IProcess. Processes are similar to tile entities except that
 * they are not bound to anything and they are not currently persistent (they will be deleted when the world closes)
 *
 * Created by brandon3055 on 12/8/2015.
 */
public class ProcessHandler {

    private static List<IProcess> processes = new ArrayList<IProcess>();
    private static List<IProcess> newProcesses = new ArrayList<IProcess>();

    public static void init() {
        FMLCommonHandler.instance().bus().register(new ProcessHandler());
        MinecraftForge.EVENT_BUS.register(new ProcessHandler());
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {

            Iterator<IProcess> i = processes.iterator();

            while (i.hasNext()) {
                IProcess process = i.next();
                if (process.isDead()) i.remove();
                else process.updateProcess();
            }

            if (!newProcesses.isEmpty()) {
                processes.addAll(newProcesses);
                newProcesses.clear();
            }
        }
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public void onWorldClose(WorldEvent.Unload event) {
        processes.clear();
        newProcesses.clear();
    }

    @SuppressWarnings("unused")
    public static void addProcess(IProcess process) {
        newProcesses.add(process);
    }
}
