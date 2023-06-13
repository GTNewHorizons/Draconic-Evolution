package com.brandon3055.draconicevolution.client.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.client.gui.componentguis.GUIManual;
import com.brandon3055.draconicevolution.client.gui.componentguis.GUIReactor;
import com.brandon3055.draconicevolution.client.gui.componentguis.GUIToolConfig;
import com.brandon3055.draconicevolution.common.container.*;
import com.brandon3055.draconicevolution.common.inventory.InventoryTool;
import com.brandon3055.draconicevolution.common.tileentities.*;
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
    public static final int GUIID_CONTAINER_TEMPLATE = 100;

    public GuiHandler() {
        NetworkRegistry.INSTANCE.registerGuiHandler(DraconicEvolution.instance, this);
    }

    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        switch (ID) {
            case GUIID_WEATHER_CONTROLLER:
                TileEntity weatherController = world.getTileEntity(x, y, z);
                if (weatherController instanceof TileWeatherController) {
                    return new ContainerWeatherController(player.inventory, (TileWeatherController) weatherController);
                }
                break;
            case GUIID_SUN_DIAL:
                TileEntity sunDial = world.getTileEntity(x, y, z);
                if (sunDial instanceof TileSunDial) {
                    return new ContainerSunDial(player.inventory, (TileSunDial) sunDial);
                }
                break;
            case GUIID_GRINDER:
                TileEntity grinder = world.getTileEntity(x, y, z);
                if (grinder instanceof TileGrinder) {
                    return new ContainerGrinder(player.inventory, (TileGrinder) grinder);
                }
                break;
            case GUIID_PLAYERDETECTOR:
                TileEntity detector = world.getTileEntity(x, y, z);
                if (detector instanceof TilePlayerDetectorAdvanced) {
                    return new ContainerPlayerDetector(player.inventory, (TilePlayerDetectorAdvanced) detector);
                }
                break;
            case GUIID_ENERGY_INFUSER:
                TileEntity infuser = world.getTileEntity(x, y, z);
                if (infuser instanceof TileEnergyInfuser) {
                    return new ContainerEnergyInfuser(player.inventory, (TileEnergyInfuser) infuser);
                }
                break;
            case GUIID_GENERATOR:
                TileEntity generator = world.getTileEntity(x, y, z);
                if (generator instanceof TileGenerator) {
                    return new ContainerGenerator(player.inventory, (TileGenerator) generator);
                }
                break;
            case GUIID_DISSENCHANTER:
                TileEntity dissenchanter = world.getTileEntity(x, y, z);
                if (dissenchanter instanceof TileDissEnchanter) {
                    return new ContainerDissEnchanter(player.inventory, (TileDissEnchanter) dissenchanter);
                }
                break;
            case GUIID_DRACONIC_CHEST:
                TileEntity containerChest = world.getTileEntity(x, y, z);
                if (containerChest instanceof TileDraconiumChest) {
                    return new ContainerDraconiumChest(player.inventory, (TileDraconiumChest) containerChest);
                }
                break;
            case GUIID_REACTOR:
                TileEntity reactor = world.getTileEntity(x, y, z);
                if (reactor instanceof TileReactorCore) {
                    return new ContainerReactor(player, (TileReactorCore) reactor);
                }
                break;
            case GUIID_TOOL_CONFIG:
                return new ContainerAdvTool(player.inventory, new InventoryTool(player, null));
            case GUIID_UPGRADE_MODIFIER:
                TileEntity containerTemp = world.getTileEntity(x, y, z);
                if (containerTemp instanceof TileUpgradeModifier) {
                    return new ContainerUpgradeModifier(player.inventory, (TileUpgradeModifier) containerTemp);
                }
                break;
        }

        return null;
    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        switch (ID) {
            case GUIID_WEATHER_CONTROLLER:
                TileEntity weatherController = world.getTileEntity(x, y, z);
                if (weatherController instanceof TileWeatherController) {
                    return new GUIWeatherController(player.inventory, (TileWeatherController) weatherController);
                }
                break;
            case GUIID_SUN_DIAL:
                TileEntity sunDial = world.getTileEntity(x, y, z);
                if (sunDial instanceof TileSunDial) {
                    return new GUISunDial(player.inventory, (TileSunDial) sunDial);
                }
                break;
            case GUIID_TELEPORTER:
                return new GUITeleporter(player);
            case GUIID_GRINDER:
                TileEntity grinder = world.getTileEntity(x, y, z);
                if (grinder instanceof TileGrinder) {
                    return new GUIGrinder(player.inventory, (TileGrinder) grinder);
                }
                break;
            case GUIID_PARTICLEGEN:
                TileEntity particleGenerator = world.getTileEntity(x, y, z);
                if (particleGenerator instanceof TileParticleGenerator) {
                    return new GUIParticleGenerator((TileParticleGenerator) particleGenerator);
                }
                break;
            case GUIID_PLAYERDETECTOR:
                TileEntity detector = world.getTileEntity(x, y, z);
                if (detector instanceof TilePlayerDetectorAdvanced) {
                    return new GUIPlayerDetector(player.inventory, (TilePlayerDetectorAdvanced) detector);
                }
                break;
            case GUIID_ENERGY_INFUSER:
                TileEntity infuser = world.getTileEntity(x, y, z);
                if (infuser instanceof TileEnergyInfuser) {
                    return new GUIEnergyInfuser(player.inventory, (TileEnergyInfuser) infuser);
                }
                break;
            case GUIID_GENERATOR:
                TileEntity generator = world.getTileEntity(x, y, z);
                if (generator instanceof TileGenerator) {
                    return new GUIGenerator(player.inventory, (TileGenerator) generator);
                }
                break;
            case GUIID_MANUAL:
                return new GUIManual();
            case GUIID_DISSENCHANTER:
                TileEntity dissenchanter = world.getTileEntity(x, y, z);
                if (dissenchanter instanceof TileDissEnchanter) {
                    return new GUIDissEnchanter(player.inventory, (TileDissEnchanter) dissenchanter);
                }
                break;
            case GUIID_DRACONIC_CHEST:
                TileEntity containerChest = world.getTileEntity(x, y, z);
                if (containerChest instanceof TileDraconiumChest) {
                    return new GUIDraconiumChest(player.inventory, (TileDraconiumChest) containerChest);
                }
                break;
            case GUIID_REACTOR:
                TileEntity reactor = world.getTileEntity(x, y, z);
                if (reactor instanceof TileReactorCore) {
                    return new GUIReactor(
                            (TileReactorCore) reactor,
                            new ContainerReactor(player, (TileReactorCore) reactor));
                }
                break;
            case GUIID_TOOL_CONFIG:
                return new GUIToolConfig(
                        player,
                        new ContainerAdvTool(player.inventory, new InventoryTool(player, null)));
            case GUIID_FLOW_GATE:
                TileEntity gate = world.getTileEntity(x, y, z);
                if (gate instanceof TileGate) {
                    return new GUIFlowGate((TileGate) world.getTileEntity(x, y, z));
                }
                break;
            case GUIID_UPGRADE_MODIFIER:
                TileEntity containerTemp = world.getTileEntity(x, y, z);
                if (containerTemp instanceof TileUpgradeModifier) {
                    return new GUIUpgradeModifier(
                            player.inventory,
                            (TileUpgradeModifier) containerTemp,
                            new ContainerUpgradeModifier(player.inventory, (TileUpgradeModifier) containerTemp));
                }
                break;
        }

        return null;
    }
}
