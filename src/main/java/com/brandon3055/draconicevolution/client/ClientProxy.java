package com.brandon3055.draconicevolution.client;

import static com.brandon3055.draconicevolution.integration.nei.IMCForNEI.IMCSender;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;

import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.client.handler.ClientEventHandler;
import com.brandon3055.draconicevolution.client.handler.HudHandler;
import com.brandon3055.draconicevolution.client.handler.ItemDisplayManager;
import com.brandon3055.draconicevolution.client.handler.ParticleHandler;
import com.brandon3055.draconicevolution.client.handler.ResourceHandler;
import com.brandon3055.draconicevolution.client.handler.ShieldRenderHandler;
import com.brandon3055.draconicevolution.client.keybinding.KeyInputHandler;
import com.brandon3055.draconicevolution.client.render.IRenderTweak;
import com.brandon3055.draconicevolution.client.render.block.RenderCrystal;
import com.brandon3055.draconicevolution.client.render.block.RenderDraconiumChest;
import com.brandon3055.draconicevolution.client.render.block.RenderEarthItem;
import com.brandon3055.draconicevolution.client.render.block.RenderEnergyInfuser;
import com.brandon3055.draconicevolution.client.render.block.RenderParticleGen;
import com.brandon3055.draconicevolution.client.render.block.RenderPortal;
import com.brandon3055.draconicevolution.client.render.block.RenderReactorCore;
import com.brandon3055.draconicevolution.client.render.block.RenderReactorEnergyInjector;
import com.brandon3055.draconicevolution.client.render.block.RenderReactorStabilizer;
import com.brandon3055.draconicevolution.client.render.block.RenderTeleporterStand;
import com.brandon3055.draconicevolution.client.render.block.RenderUpgradeModifier;
import com.brandon3055.draconicevolution.client.render.entity.RenderChaosCrystal;
import com.brandon3055.draconicevolution.client.render.entity.RenderDragon;
import com.brandon3055.draconicevolution.client.render.entity.RenderDragonHeart;
import com.brandon3055.draconicevolution.client.render.entity.RenderDragonProjectile;
import com.brandon3055.draconicevolution.client.render.entity.RenderEntityChaosVortex;
import com.brandon3055.draconicevolution.client.render.entity.RenderEntityCustomArrow;
import com.brandon3055.draconicevolution.client.render.item.RenderArmor;
import com.brandon3055.draconicevolution.client.render.item.RenderBow;
import com.brandon3055.draconicevolution.client.render.item.RenderBowModel;
import com.brandon3055.draconicevolution.client.render.item.RenderChaosFragment;
import com.brandon3055.draconicevolution.client.render.item.RenderChaosShard;
import com.brandon3055.draconicevolution.client.render.item.RenderMobSoul;
import com.brandon3055.draconicevolution.client.render.item.RenderStabilizerPart;
import com.brandon3055.draconicevolution.client.render.item.RenderTool;
import com.brandon3055.draconicevolution.client.render.particle.ParticleEnergyBeam;
import com.brandon3055.draconicevolution.client.render.particle.ParticleEnergyField;
import com.brandon3055.draconicevolution.client.render.particle.ParticleReactorBeam;
import com.brandon3055.draconicevolution.client.render.tile.RenderTileChaosShard;
import com.brandon3055.draconicevolution.client.render.tile.RenderTileCrystal;
import com.brandon3055.draconicevolution.client.render.tile.RenderTileCustomSpawner;
import com.brandon3055.draconicevolution.client.render.tile.RenderTileDissEnchanter;
import com.brandon3055.draconicevolution.client.render.tile.RenderTileDraconiumChest;
import com.brandon3055.draconicevolution.client.render.tile.RenderTileEarth;
import com.brandon3055.draconicevolution.client.render.tile.RenderTileEnergyInfiser;
import com.brandon3055.draconicevolution.client.render.tile.RenderTileEnergyPylon;
import com.brandon3055.draconicevolution.client.render.tile.RenderTileEnergyStorageCore;
import com.brandon3055.draconicevolution.client.render.tile.RenderTileParticleGen;
import com.brandon3055.draconicevolution.client.render.tile.RenderTilePlacedItem;
import com.brandon3055.draconicevolution.client.render.tile.RenderTileReactorCore;
import com.brandon3055.draconicevolution.client.render.tile.RenderTileReactorEnergyInjector;
import com.brandon3055.draconicevolution.client.render.tile.RenderTileReactorStabilizer;
import com.brandon3055.draconicevolution.client.render.tile.RenderTileTeleporterStand;
import com.brandon3055.draconicevolution.client.render.tile.RenderTileUpgradeModifier;
import com.brandon3055.draconicevolution.common.CommonProxy;
import com.brandon3055.draconicevolution.common.ModBlocks;
import com.brandon3055.draconicevolution.common.ModItems;
import com.brandon3055.draconicevolution.common.blocks.multiblock.IReactorPart;
import com.brandon3055.draconicevolution.common.entity.EntityChaosCrystal;
import com.brandon3055.draconicevolution.common.entity.EntityChaosGuardian;
import com.brandon3055.draconicevolution.common.entity.EntityChaosVortex;
import com.brandon3055.draconicevolution.common.entity.EntityCustomArrow;
import com.brandon3055.draconicevolution.common.entity.EntityCustomDragon;
import com.brandon3055.draconicevolution.common.entity.EntityDragonHeart;
import com.brandon3055.draconicevolution.common.entity.EntityDragonProjectile;
import com.brandon3055.draconicevolution.common.handler.ConfigHandler;
import com.brandon3055.draconicevolution.common.lib.References;
import com.brandon3055.draconicevolution.common.tileentities.TileChaosShard;
import com.brandon3055.draconicevolution.common.tileentities.TileCustomSpawner;
import com.brandon3055.draconicevolution.common.tileentities.TileDislocatorInhibitor;
import com.brandon3055.draconicevolution.common.tileentities.TileDissEnchanter;
import com.brandon3055.draconicevolution.common.tileentities.TileDraconiumChest;
import com.brandon3055.draconicevolution.common.tileentities.TileEnergyInfuser;
import com.brandon3055.draconicevolution.common.tileentities.TileParticleGenerator;
import com.brandon3055.draconicevolution.common.tileentities.TilePlacedItem;
import com.brandon3055.draconicevolution.common.tileentities.TileTeleporterStand;
import com.brandon3055.draconicevolution.common.tileentities.TileUpgradeModifier;
import com.brandon3055.draconicevolution.common.tileentities.energynet.TileEnergyRelay;
import com.brandon3055.draconicevolution.common.tileentities.energynet.TileEnergyTransceiver;
import com.brandon3055.draconicevolution.common.tileentities.energynet.TileWirelessEnergyTransceiver;
import com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.TileEarth;
import com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.TileEnergyPylon;
import com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.TileEnergyStorageCore;
import com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.reactor.TileReactorCore;
import com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.reactor.TileReactorEnergyInjector;
import com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.reactor.TileReactorStabilizer;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;

public final class ClientProxy extends CommonProxy {

    private static final boolean debug = DraconicEvolution.debug;

    private final Map<World, List<TileDislocatorInhibitor>> clientInhibitorsMap = new HashMap<>();
    private ClientEventHandler clientHandler;
    private KeyInputHandler keybindHandler;
    private ItemDisplayManager itemDisplay;
    private ShieldRenderHandler shieldRenderer;

    public ClientProxy() {
        FMLCommonHandler.instance().bus().register(this);
    }

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        if (debug) System.out.println("on Client side");
        super.preInit(event);

        ResourceHandler.init(event);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        this.keybindHandler = new KeyInputHandler();
        FMLCommonHandler.instance().bus().register(this.keybindHandler);
        this.clientHandler = new ClientEventHandler();
        FMLCommonHandler.instance().bus().register(this.clientHandler);
        MinecraftForge.EVENT_BUS.register(this.clientHandler);
        final HudHandler hudHandler = new HudHandler();
        FMLCommonHandler.instance().bus().register(hudHandler);
        MinecraftForge.EVENT_BUS.register(hudHandler);
        registerRenderIDs();
        registerRendering();
        ResourceHandler.instance.tick(null);
        IMCSender();
        this.itemDisplay = new ItemDisplayManager(60);
        FMLCommonHandler.instance().bus().register(this.itemDisplay);
        MinecraftForge.EVENT_BUS.register(this.itemDisplay);
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
        ResourceHandler.instance.tick(null);
    }

    @SubscribeEvent
    public void onClientConnect(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        this.shieldRenderer = new ShieldRenderHandler();
        FMLCommonHandler.instance().bus().register(this.shieldRenderer);
        MinecraftForge.EVENT_BUS.register(this.shieldRenderer);
    }

    @SubscribeEvent
    public void onClientDisconnect(final FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        this.clientInhibitorsMap.clear();
        this.itemDisplay.startDrawing(null);
        if (this.shieldRenderer != null) {
            FMLCommonHandler.instance().bus().unregister(this.shieldRenderer);
            MinecraftForge.EVENT_BUS.unregister(this.shieldRenderer);
            this.shieldRenderer = null;
        }
    }

    @Override
    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.world.isRemote) {
            this.clientInhibitorsMap.remove(event.world);
        } else {
            super.onWorldUnload(event);
        }
    }

    private void registerRendering() {
        // Item Renderers
        MinecraftForgeClient.registerItemRenderer(ModItems.wyvernBow, new RenderBow());
        MinecraftForgeClient.registerItemRenderer(ModItems.draconicBow, new RenderBow());
        MinecraftForgeClient.registerItemRenderer(ModItems.mobSoul, new RenderMobSoul());
        MinecraftForgeClient.registerItemRenderer(ModItems.chaosShard, new RenderChaosShard());
        MinecraftForgeClient.registerItemRenderer(ModItems.reactorStabilizerParts, new RenderStabilizerPart());
        MinecraftForgeClient.registerItemRenderer(ModItems.chaosFragment, new RenderChaosFragment());

        if (!ConfigHandler.useOldArmorModel) {
            MinecraftForgeClient.registerItemRenderer(ModItems.wyvernHelm, new RenderArmor(ModItems.wyvernHelm));
            MinecraftForgeClient.registerItemRenderer(ModItems.wyvernChest, new RenderArmor(ModItems.wyvernChest));
            MinecraftForgeClient.registerItemRenderer(ModItems.wyvernLeggs, new RenderArmor(ModItems.wyvernLeggs));
            MinecraftForgeClient.registerItemRenderer(ModItems.wyvernBoots, new RenderArmor(ModItems.wyvernBoots));
            MinecraftForgeClient.registerItemRenderer(ModItems.draconicHelm, new RenderArmor(ModItems.draconicHelm));
            MinecraftForgeClient.registerItemRenderer(ModItems.draconicChest, new RenderArmor(ModItems.draconicChest));
            MinecraftForgeClient.registerItemRenderer(ModItems.draconicLeggs, new RenderArmor(ModItems.draconicLeggs));
            MinecraftForgeClient.registerItemRenderer(ModItems.draconicBoots, new RenderArmor(ModItems.draconicBoots));
        }

        if (!ConfigHandler.useOldD2DToolTextures) {
            MinecraftForgeClient.registerItemRenderer(
                    ModItems.draconicSword,
                    new RenderTool(
                            "models/tools/DraconicSword.obj",
                            "textures/models/tools/DraconicSword.png",
                            (IRenderTweak) ModItems.draconicSword));
            MinecraftForgeClient.registerItemRenderer(
                    ModItems.wyvernPickaxe,
                    new RenderTool(
                            "models/tools/Pickaxe.obj",
                            "textures/models/tools/Pickaxe.png",
                            (IRenderTweak) ModItems.wyvernPickaxe));
            MinecraftForgeClient.registerItemRenderer(
                    ModItems.draconicPickaxe,
                    new RenderTool(
                            "models/tools/DraconicPickaxe.obj",
                            "textures/models/tools/DraconicPickaxe.png",
                            (IRenderTweak) ModItems.draconicPickaxe));
            MinecraftForgeClient.registerItemRenderer(
                    ModItems.draconicAxe,
                    new RenderTool(
                            "models/tools/DraconicLumberAxe.obj",
                            "textures/models/tools/DraconicLumberAxe.png",
                            (IRenderTweak) ModItems.draconicAxe));
            MinecraftForgeClient.registerItemRenderer(
                    ModItems.wyvernShovel,
                    new RenderTool(
                            "models/tools/Shovel.obj",
                            "textures/models/tools/Shovel.png",
                            (IRenderTweak) ModItems.wyvernShovel));
            MinecraftForgeClient.registerItemRenderer(
                    ModItems.draconicShovel,
                    new RenderTool(
                            "models/tools/DraconicShovel.obj",
                            "textures/models/tools/DraconicShovel.png",
                            (IRenderTweak) ModItems.draconicShovel));
            MinecraftForgeClient.registerItemRenderer(
                    ModItems.wyvernSword,
                    new RenderTool(
                            "models/tools/Sword.obj",
                            "textures/models/tools/Sword.png",
                            (IRenderTweak) ModItems.wyvernSword));
            MinecraftForgeClient.registerItemRenderer(
                    ModItems.draconicDestructionStaff,
                    new RenderTool(
                            "models/tools/DraconicStaffOfPower.obj",
                            "textures/models/tools/DraconicStaffOfPower.png",
                            (IRenderTweak) ModItems.draconicDestructionStaff));
            MinecraftForgeClient.registerItemRenderer(
                    ModItems.draconicHoe,
                    new RenderTool(
                            "models/tools/DraconicHoe.obj",
                            "textures/models/tools/DraconicHoe.png",
                            (IRenderTweak) ModItems.draconicHoe));
            MinecraftForgeClient.registerItemRenderer(ModItems.draconicBow, new RenderBowModel(true));
            MinecraftForgeClient.registerItemRenderer(ModItems.wyvernBow, new RenderBowModel(false));
        }

        MinecraftForgeClient
                .registerItemRenderer(Item.getItemFromBlock(ModBlocks.draconiumChest), new RenderDraconiumChest());
        MinecraftForgeClient
                .registerItemRenderer(Item.getItemFromBlock(ModBlocks.particleGenerator), new RenderParticleGen());
        MinecraftForgeClient
                .registerItemRenderer(Item.getItemFromBlock(ModBlocks.energyInfuser), new RenderEnergyInfuser());
        MinecraftForgeClient.registerItemRenderer(Item.getItemFromBlock(ModBlocks.energyCrystal), new RenderCrystal());
        MinecraftForgeClient.registerItemRenderer(
                Item.getItemFromBlock(ModBlocks.reactorStabilizer),
                new RenderReactorStabilizer());
        MinecraftForgeClient.registerItemRenderer(
                Item.getItemFromBlock(ModBlocks.reactorEnergyInjector),
                new RenderReactorEnergyInjector());
        MinecraftForgeClient
                .registerItemRenderer(Item.getItemFromBlock(ModBlocks.reactorCore), new RenderReactorCore());
        MinecraftForgeClient.registerItemRenderer(Item.getItemFromBlock(ModBlocks.earthBlock), new RenderEarthItem());
        MinecraftForgeClient
                .registerItemRenderer(Item.getItemFromBlock(ModBlocks.chaosCrystal), new RenderChaosShard());
        MinecraftForgeClient
                .registerItemRenderer(Item.getItemFromBlock(ModBlocks.upgradeModifier), new RenderUpgradeModifier());

        // ISimpleBlockRendering
        RenderingRegistry.registerBlockHandler(new RenderTeleporterStand());
        RenderingRegistry.registerBlockHandler(new RenderPortal());

        // TileEntitySpecialRenderers
        ClientRegistry.bindTileEntitySpecialRenderer(TileParticleGenerator.class, new RenderTileParticleGen());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEnergyInfuser.class, new RenderTileEnergyInfiser());
        ClientRegistry.bindTileEntitySpecialRenderer(TileCustomSpawner.class, new RenderTileCustomSpawner());
        // ClientRegistry.bindTileEntitySpecialRenderer(TileTestBlock.class, new RenderTileCrystal());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEnergyStorageCore.class, new RenderTileEnergyStorageCore());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEarth.class, new RenderTileEarth());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEnergyPylon.class, new RenderTileEnergyPylon());
        ClientRegistry.bindTileEntitySpecialRenderer(TilePlacedItem.class, new RenderTilePlacedItem());
        ClientRegistry.bindTileEntitySpecialRenderer(TileDissEnchanter.class, new RenderTileDissEnchanter());
        ClientRegistry.bindTileEntitySpecialRenderer(TileTeleporterStand.class, new RenderTileTeleporterStand());
        ClientRegistry.bindTileEntitySpecialRenderer(TileDraconiumChest.class, new RenderTileDraconiumChest());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEnergyRelay.class, new RenderTileCrystal());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEnergyTransceiver.class, new RenderTileCrystal());
        ClientRegistry.bindTileEntitySpecialRenderer(TileWirelessEnergyTransceiver.class, new RenderTileCrystal());
        ClientRegistry.bindTileEntitySpecialRenderer(TileReactorCore.class, new RenderTileReactorCore());
        ClientRegistry.bindTileEntitySpecialRenderer(TileReactorStabilizer.class, new RenderTileReactorStabilizer());
        ClientRegistry
                .bindTileEntitySpecialRenderer(TileReactorEnergyInjector.class, new RenderTileReactorEnergyInjector());
        ClientRegistry.bindTileEntitySpecialRenderer(TileChaosShard.class, new RenderTileChaosShard());
        ClientRegistry.bindTileEntitySpecialRenderer(TileUpgradeModifier.class, new RenderTileUpgradeModifier());

        // Entitys
        RenderingRegistry.registerEntityRenderingHandler(EntityCustomDragon.class, new RenderDragon());
        RenderingRegistry.registerEntityRenderingHandler(EntityChaosGuardian.class, new RenderDragon());
        RenderingRegistry.registerEntityRenderingHandler(EntityDragonHeart.class, new RenderDragonHeart());
        RenderingRegistry.registerEntityRenderingHandler(EntityDragonProjectile.class, new RenderDragonProjectile());
        RenderingRegistry.registerEntityRenderingHandler(EntityChaosCrystal.class, new RenderChaosCrystal());
        RenderingRegistry.registerEntityRenderingHandler(EntityChaosVortex.class, new RenderEntityChaosVortex());
        RenderingRegistry.registerEntityRenderingHandler(EntityCustomArrow.class, new RenderEntityCustomArrow());
    }

    private void registerRenderIDs() {
        References.idTeleporterStand = RenderingRegistry.getNextAvailableRenderId();
        References.idPortal = RenderingRegistry.getNextAvailableRenderId();
    }

    public ParticleEnergyBeam energyBeam(World worldObj, double x, double y, double z, double tx, double ty, double tz,
            int powerFlow, boolean advanced, ParticleEnergyBeam oldBeam, boolean render, int beamType) {
        if (!worldObj.isRemote) return null;
        ParticleEnergyBeam beam = oldBeam;
        boolean inRange = ParticleHandler.isInRange(x, y, z, 50) || ParticleHandler.isInRange(tx, ty, tz, 50);

        if (beam == null || beam.isDead) {
            if (inRange) {
                beam = new ParticleEnergyBeam(worldObj, x, y, z, tx, ty, tz, 8, powerFlow, advanced, beamType);

                FMLClientHandler.instance().getClient().effectRenderer.addEffect(beam);
            }
        } else if (!inRange) {
            beam.setDead();
            return null;
        } else {
            beam.update(powerFlow, render);
        }
        return beam;
    }

    public ParticleEnergyField energyField(World worldObj, double x, double y, double z, int type, boolean advanced,
            ParticleEnergyField oldBeam, boolean render) {
        if (!worldObj.isRemote) return null;
        ParticleEnergyField beam = oldBeam;
        boolean inRange = ParticleHandler.isInRange(x, y, z, 50);

        if (beam == null || beam.isDead) {
            if (inRange) {
                beam = new ParticleEnergyField(worldObj, x, y, z, 8, type, advanced);

                FMLClientHandler.instance().getClient().effectRenderer.addEffect(beam);
            }
        } else if (!inRange) {
            beam.setDead();
            return null;
        } else {
            beam.update(render);
        }
        return beam;
    }

    public ParticleReactorBeam reactorBeam(TileEntity tile, ParticleReactorBeam oldBeam, boolean render) {
        if (!tile.getWorldObj().isRemote || !(tile instanceof IReactorPart)) return null;
        ParticleReactorBeam beam = oldBeam;
        boolean inRange = ParticleHandler.isInRange(tile.xCoord, tile.yCoord, tile.zCoord, 50);

        if (beam == null || beam.isDead) {
            if (inRange) {
                beam = new ParticleReactorBeam(tile);

                FMLClientHandler.instance().getClient().effectRenderer.addEffect(beam);
            }
        } else if (!inRange) {
            beam.setDead();
            return null;
        } else {
            beam.update();
        }
        return beam;
    }

    public void spawnParticle(EntityFX particle, int range) {
        if (particle.worldObj.isRemote) {
            ParticleHandler.spawnCustomParticle(particle, range);
        }
    }

    public ISound playISound(ISound sound) {
        FMLClientHandler.instance().getClient().getSoundHandler().playSound(sound);
        return sound;
    }

    @Override
    public boolean isDedicatedServer() {
        return false;
    }

    @Override
    public World getClientWorld() {
        return Minecraft.getMinecraft().theWorld;
    }

    @Override
    public boolean isSpaceDown() {
        return Minecraft.getMinecraft().gameSettings.keyBindJump.getIsKeyPressed();
    }

    @Override
    public boolean isShiftDown() {
        return Minecraft.getMinecraft().gameSettings.keyBindSneak.getIsKeyPressed();
    }

    @Override
    public boolean isCtrlDown() {
        return Minecraft.getMinecraft().gameSettings.keyBindSprint.getIsKeyPressed();
    }

    @Override
    public EntityPlayer getClientPlayer() {
        return Minecraft.getMinecraft().thePlayer;
    }

    @Override
    public boolean isClient() {
        return true;
    }

    public KeyInputHandler getKeybindHandler() {
        return keybindHandler;
    }

    public void displayItem(ItemStack stack) {
        this.itemDisplay.startDrawing(stack);
    }

    public int getElapsedTicks() {
        return this.clientHandler.getElapsedTicks();
    }

    public float getEnergyCrystalAlpha() {
        return this.clientHandler.getEnergyCrystalAlpha();
    }

    public void tryRepositionPlayerOnMount(int entityId) {
        this.clientHandler.tryRepositionPlayerOnMount(entityId);
    }

    public void renderShield(EntityPlayer player, float shieldPowerF) {
        if (this.shieldRenderer != null) {
            this.shieldRenderer.renderShield(player, shieldPowerF);
        }
    }

    @Override
    public void registerInhibitor(TileDislocatorInhibitor tile) {
        if (tile.hasWorldObj() && tile.getWorldObj().isRemote) {
            registerInhibitor(this.clientInhibitorsMap, tile);
        } else {
            super.registerInhibitor(tile);
        }
    }

    @Override
    public void unregisterInhibitor(TileDislocatorInhibitor tile) {
        if (tile.hasWorldObj() && tile.getWorldObj().isRemote) {
            unregisterInhibitor(this.clientInhibitorsMap, tile);
        } else {
            super.unregisterInhibitor(tile);
        }
    }

    @Override
    public boolean isBlockedByInhibitor(World world, EntityItem item) {
        if (world.isRemote) {
            return isBlockedByInhibitor(this.clientInhibitorsMap, world, item);
        } else {
            return super.isBlockedByInhibitor(world, item);
        }
    }
}
