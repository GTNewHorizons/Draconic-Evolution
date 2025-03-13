package com.brandon3055.draconicevolution.client.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.client.gui.componentguis.GUIManual;
import com.brandon3055.draconicevolution.client.gui.componentguis.GUIReactor;
import com.brandon3055.draconicevolution.client.gui.componentguis.GUIToolConfig;
import com.brandon3055.draconicevolution.common.container.ContainerAdvTool;
import com.brandon3055.draconicevolution.common.container.ContainerDislocatorInhibitor;
import com.brandon3055.draconicevolution.common.container.ContainerDissEnchanter;
import com.brandon3055.draconicevolution.common.container.ContainerDraconiumChest;
import com.brandon3055.draconicevolution.common.container.ContainerEnergyInfuser;
import com.brandon3055.draconicevolution.common.container.ContainerGenerator;
import com.brandon3055.draconicevolution.common.container.ContainerGrinder;
import com.brandon3055.draconicevolution.common.container.ContainerPlayerDetector;
import com.brandon3055.draconicevolution.common.container.ContainerReactor;
import com.brandon3055.draconicevolution.common.container.ContainerSunDial;
import com.brandon3055.draconicevolution.common.container.ContainerUpgradeModifier;
import com.brandon3055.draconicevolution.common.container.ContainerWeatherController;
import com.brandon3055.draconicevolution.common.inventory.InventoryTool;
import com.brandon3055.draconicevolution.common.tileentities.TileDislocatorInhibitor;
import com.brandon3055.draconicevolution.common.tileentities.TileDissEnchanter;
import com.brandon3055.draconicevolution.common.tileentities.TileDraconiumChest;
import com.brandon3055.draconicevolution.common.tileentities.TileEnergyInfuser;
import com.brandon3055.draconicevolution.common.tileentities.TileGenerator;
import com.brandon3055.draconicevolution.common.tileentities.TileGrinder;
import com.brandon3055.draconicevolution.common.tileentities.TileParticleGenerator;
import com.brandon3055.draconicevolution.common.tileentities.TilePlayerDetectorAdvanced;
import com.brandon3055.draconicevolution.common.tileentities.TileSunDial;
import com.brandon3055.draconicevolution.common.tileentities.TileUpgradeModifier;
import com.brandon3055.draconicevolution.common.tileentities.TileWeatherController;
import com.brandon3055.draconicevolution.common.tileentities.gates.TileGate;
import com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.reactor.TileReactorCore;

import cpw.mods.fml.common.network.IGuiHandler;
import cpw.mods.fml.common.network.NetworkRegistry;

public class GuiHandler implements IGuiHandler {

    public static final int GUIID_WEATHER_CONTROLLER = 0;
    public static final int GUIID_SUN_DIAL = 1;
    public static final int GUIID_GRINDER = 2;
    public static final int GUIID_TELEPORTER = 3;
    public static final int GUIID_PARTICLEGEN = 5;
    public static final int GUIID_PLAYERDETECTOR = 6;
    public static final int GUIID_ENERGY_INFUSER = 7;
    public static final int GUIID_GENERATOR = 8;
    public static final int GUIID_MANUAL = 9;
    public static final int GUIID_DISSENCHANTER = 10;
    public static final int GUIID_DRACONIC_CHEST = 11;
    public static final int GUIID_TOOL_CONFIG = 12;
    public static final int GUIID_FLOW_GATE = 13;
    public static final int GUIID_REACTOR = 14;
    public static final int GUIID_UPGRADE_MODIFIER = 15;
    public static final int GUIID_DISLOCATOR_INHIBITOR = 16;
    public static final int GUIID_CONTAINER_TEMPLATE = 100;

    public GuiHandler() {
        NetworkRegistry.INSTANCE.registerGuiHandler(DraconicEvolution.instance, this);
    }

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        switch (id) {
            case GUIID_WEATHER_CONTROLLER -> {
                if (tile instanceof TileWeatherController weatherController) {
                    return new ContainerWeatherController(player.inventory, weatherController);
                }
            }
            case GUIID_SUN_DIAL -> {
                if (tile instanceof TileSunDial sunDial) {
                    return new ContainerSunDial(player.inventory, sunDial);
                }
            }
            case GUIID_GRINDER -> {
                if (tile instanceof TileGrinder grinder) {
                    return new ContainerGrinder(player.inventory, grinder);
                }
            }
            case GUIID_PLAYERDETECTOR -> {
                if (tile instanceof TilePlayerDetectorAdvanced detector) {
                    return new ContainerPlayerDetector(player.inventory, detector);
                }
            }
            case GUIID_ENERGY_INFUSER -> {
                if (tile instanceof TileEnergyInfuser infuser) {
                    return new ContainerEnergyInfuser(player.inventory, infuser);
                }
            }
            case GUIID_GENERATOR -> {
                if (tile instanceof TileGenerator generator) {
                    return new ContainerGenerator(player.inventory, generator);
                }
            }
            case GUIID_DISSENCHANTER -> {
                if (tile instanceof TileDissEnchanter dissenchanter) {
                    return new ContainerDissEnchanter(player.inventory, dissenchanter);
                }
            }
            case GUIID_DRACONIC_CHEST -> {
                if (tile instanceof TileDraconiumChest draconiumChest) {
                    return new ContainerDraconiumChest(player.inventory, draconiumChest);
                }
            }
            case GUIID_REACTOR -> {
                if (tile instanceof TileReactorCore reactorCore) {
                    return new ContainerReactor(player, reactorCore);
                }
            }
            case GUIID_TOOL_CONFIG -> {
                return new ContainerAdvTool(player.inventory, new InventoryTool(player, null));
            }
            case GUIID_UPGRADE_MODIFIER -> {
                if (tile instanceof TileUpgradeModifier upgradeModifier) {
                    return new ContainerUpgradeModifier(player.inventory, upgradeModifier);
                }
            }
            case GUIID_DISLOCATOR_INHIBITOR -> {
                if (tile instanceof TileDislocatorInhibitor inhibitor) {
                    return new ContainerDislocatorInhibitor(player.inventory, inhibitor);
                }
            }
        }

        return null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        switch (id) {
            case GUIID_WEATHER_CONTROLLER -> {
                if (tile instanceof TileWeatherController weatherController) {
                    return new GUIWeatherController(player.inventory, weatherController);
                }
            }
            case GUIID_SUN_DIAL -> {
                if (tile instanceof TileSunDial sunDial) {
                    return new GUISunDial(player.inventory, sunDial);
                }
            }
            case GUIID_TELEPORTER -> {
                return new GUITeleporter(player);
            }
            case GUIID_GRINDER -> {
                if (tile instanceof TileGrinder grinder) {
                    return new GUIGrinder(player.inventory, grinder);
                }
            }
            case GUIID_PARTICLEGEN -> {
                if (tile instanceof TileParticleGenerator particleGenerator) {
                    return new GUIParticleGenerator(particleGenerator);
                }
            }
            case GUIID_PLAYERDETECTOR -> {
                if (tile instanceof TilePlayerDetectorAdvanced detector) {
                    return new GUIPlayerDetector(player.inventory, detector);
                }
            }
            case GUIID_ENERGY_INFUSER -> {
                if (tile instanceof TileEnergyInfuser infuser) {
                    return new GUIEnergyInfuser(player.inventory, infuser);
                }
            }
            case GUIID_GENERATOR -> {
                if (tile instanceof TileGenerator generator) {
                    return new GUIGenerator(player.inventory, generator);
                }
            }
            case GUIID_MANUAL -> {
                return new GUIManual();
            }
            case GUIID_DISSENCHANTER -> {
                if (tile instanceof TileDissEnchanter dissenchanter) {
                    return new GUIDissEnchanter(player.inventory, dissenchanter);
                }
            }
            case GUIID_DRACONIC_CHEST -> {
                if (tile instanceof TileDraconiumChest draconiumChest) {
                    return new GUIDraconiumChest(player.inventory, draconiumChest);
                }
            }
            case GUIID_REACTOR -> {
                if (tile instanceof TileReactorCore reactorCore) {
                    return new GUIReactor(reactorCore, new ContainerReactor(player, reactorCore));
                }
            }
            case GUIID_TOOL_CONFIG -> {
                return new GUIToolConfig(
                        player,
                        new ContainerAdvTool(player.inventory, new InventoryTool(player, null)));
            }
            case GUIID_FLOW_GATE -> {
                if (tile instanceof TileGate gate) {
                    return new GUIFlowGate(gate);
                }
            }
            case GUIID_UPGRADE_MODIFIER -> {
                if (tile instanceof TileUpgradeModifier upgradeModifier) {
                    return new GUIUpgradeModifier(
                            player.inventory,
                            upgradeModifier,
                            new ContainerUpgradeModifier(player.inventory, upgradeModifier));
                }
            }
            case GUIID_DISLOCATOR_INHIBITOR -> {
                if (tile instanceof TileDislocatorInhibitor inhibitor) {
                    return new GUIDislocatorInhibitor(player.inventory, inhibitor);
                }
            }
        }

        return null;
    }
}
