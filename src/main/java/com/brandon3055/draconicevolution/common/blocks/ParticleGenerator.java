package com.brandon3055.draconicevolution.common.blocks;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.client.gui.GuiHandler;
import com.brandon3055.draconicevolution.client.handler.ParticleHandler;
import com.brandon3055.draconicevolution.client.render.particle.ParticleCustom;
import com.brandon3055.draconicevolution.common.ModBlocks;
import com.brandon3055.draconicevolution.common.lib.References;
import com.brandon3055.draconicevolution.common.lib.Strings;
import com.brandon3055.draconicevolution.common.tileentities.TileParticleGenerator;
import com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.TileEnergyStorageCore;

import cpw.mods.fml.common.network.internal.FMLNetworkHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ParticleGenerator extends BlockDE implements ITileEntityProvider {

    public static Block instance;

    public ParticleGenerator() {
        this.setBlockName(Strings.particleGeneratorName);
        this.setCreativeTab(DraconicEvolution.tabBlocksItems);
        this.setStepSound(soundTypeStone);
        this.setLightOpacity(0);
        ModBlocks.register(this);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister iconRegister) {
        blockIcon = iconRegister.registerIcon(References.RESOURCESPREFIX + "machine_side");
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float subX,
            float subY, float subZ) {
        if (world.getBlockMetadata(x, y, z) == 1) {
            return false;
        }
        if (player.getHeldItem() != null && player.getHeldItem().getItem() == Items.paper) {
            TileEntity tile = world.getTileEntity(x, y, z);
            if (tile instanceof TileParticleGenerator particleGenerator) {
                ItemStack stack = player.getHeldItem();
                if (stack.hasTagCompound() && stack.getTagCompound().hasKey("particles_enabled")) {
                    particleGenerator.setBlockNBT(stack.getTagCompound());
                    return true;
                }
            }
        }

        if (player.isSneaking()) {
            if (activateEnergyStorageCore(world, x, y, z, player)) {
                return true;
            }
            TileEntity tile = world.getTileEntity(x, y, z);
            if (tile instanceof TileParticleGenerator particleGenerator) {
                particleGenerator.toggleInverted();
            }
        } else {
            FMLNetworkHandler.openGui(player, DraconicEvolution.instance, GuiHandler.GUIID_PARTICLEGEN, world, x, y, z);
        }
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderType() {
        return -1;
    }

    @Override
    public void onNeighborBlockChange(final World world, final int x, final int y, final int z, final Block block) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileParticleGenerator particleGenerator) {
            particleGenerator.hasRedstoneSignal = world.isBlockIndirectlyGettingPowered(x, y, z);
            world.markBlockForUpdate(x, y, z);
        }
    }

    @Override
    public boolean canConnectRedstone(IBlockAccess world, int x, int y, int z, int side) {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onBlockDestroyedByPlayer(World world, int x, int y, int z, int metadata) {
        if (!world.isRemote) {
            return;
        }
        Random rand = world.rand;
        float modifier = 0.1F;
        float scale = 1;
        double spawnX = x + 0.5D;
        double spawnY = y + 0.5D;
        double spawnZ = z + 0.5D;

        for (int i = 0; i < 100; i++) {
            float motionX = modifier - ((2f * modifier) * rand.nextFloat());
            float motionY = modifier - ((2f * modifier) * rand.nextFloat());
            float motionZ = modifier - ((2f * modifier) * rand.nextFloat());

            {
                ParticleCustom particle = new ParticleCustom(
                        world,
                        spawnX,
                        spawnY,
                        spawnZ,
                        motionX,
                        motionY,
                        motionZ,
                        scale,
                        false,
                        1);
                particle.red = rand.nextInt(255);
                particle.green = rand.nextInt(255);
                particle.blue = rand.nextInt(255);
                particle.maxAge = rand.nextInt(10);
                particle.fadeTime = 20;
                particle.fadeLength = 20;
                particle.gravity = 0F;

                ParticleHandler.spawnCustomParticle(particle);
            }
        }
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public boolean shouldSideBeRendered(IBlockAccess worldIn, int x, int y, int z, int side) {
        return false;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean hasTileEntity(int metadata) {
        return true;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new TileParticleGenerator();
    }

    private boolean activateEnergyStorageCore(World world, int x, int y, int z, EntityPlayer player) {
        final ForgeDirection[] horizontalDirections = new ForgeDirection[] { ForgeDirection.EAST, ForgeDirection.SOUTH,
                ForgeDirection.WEST, ForgeDirection.NORTH };
        for (ForgeDirection direction : horizontalDirections) {
            for (int distance = 1; distance <= TileEnergyStorageCore.STABILIZER_SEARCH_DISTANCE; distance++) {
                int coreX = x + direction.offsetX * distance;
                int coreZ = z + direction.offsetZ * distance;
                if (world.getBlock(coreX, y, coreZ) == ModBlocks.energyStorageCore) {
                    return tryActivateEnergyStorageCore(world, coreX, y, coreZ, player);
                }
            }
        }
        return false;
    }

    private boolean tryActivateEnergyStorageCore(World world, int coreX, int coreY, int coreZ, EntityPlayer player) {
        TileEntity tile = world.getTileEntity(coreX, coreY, coreZ);
        if (tile instanceof TileEnergyStorageCore core) {
            if (core.tryActivate(player.capabilities.isCreativeMode)) {
                return true;
            }
            if (world.isRemote) {
                player.addChatComponentMessage(new ChatComponentTranslation("msg.energyStorageCoreUTA.txt"));
            }
        }
        return false;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block blockBroken, int metadata) {
        if (metadata == 1) {
            TileEntity tile = world.getTileEntity(x, y, z);
            if (tile instanceof TileParticleGenerator particleGenerator) {
                if (particleGenerator.getMaster() != null) {
                    world.setBlockMetadataWithNotify(x, y, z, 0, 2);
                    particleGenerator.getMaster().validateStructure(true);
                }
            }
        }
        super.breakBlock(world, x, y, z, blockBroken, metadata);
    }
}
