package com.brandon3055.draconicevolution.common.blocks.multiblock;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.common.ModBlocks;
import com.brandon3055.draconicevolution.common.blocks.BlockDE;
import com.brandon3055.draconicevolution.common.lib.References;
import com.brandon3055.draconicevolution.common.lib.Strings;
import com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.TileEnergyPylon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Created by Brandon on 28/07/2014.
 */
public class EnergyPylon extends BlockDE implements ITileEntityProvider {

    @SideOnly(Side.CLIENT)
    public IIcon icon_active_face;
    @SideOnly(Side.CLIENT)
    public IIcon icon_input;
    @SideOnly(Side.CLIENT)
    public IIcon icon_output;

    public EnergyPylon() {
        super(Material.iron);
        this.setHardness(10F);
        this.setResistance(20F);
        this.setCreativeTab(DraconicEvolution.tabBlocksItems);
        this.setBlockName(Strings.energyPylonName);
        ModBlocks.register(this);
    }

    @Override
    public boolean hasTileEntity(int metadata) {
        return metadata == 1 || metadata == 2;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return metadata == 1 || metadata == 2 ? new TileEnergyPylon() : null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister iconRegister) {
        icon_input = iconRegister.registerIcon(References.RESOURCESPREFIX + "energy_pylon_input");
        icon_output = iconRegister.registerIcon(References.RESOURCESPREFIX + "energy_pylon_output");
        icon_active_face = iconRegister.registerIcon(References.RESOURCESPREFIX + "energy_pylon_active_face");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        return metadata == 1 && side == 1 || metadata == 2 && side == 0 ? icon_active_face : icon_input;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
        int metadata = world.getBlockMetadata(x, y, z);
        if (metadata == 1 && side == 1 || metadata == 2 && side == 0) return icon_active_face;
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileEnergyPylon pylon) {
            return pylon.isReceivingEnergy ? icon_input : icon_output;
        }
        return icon_input;
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        updateBlockState(world, x, y, z);
    }

    private void updateBlockState(World world, int x, int y, int z) {
        int metadata = world.getBlockMetadata(x, y, z);
        if (metadata == 0) {
            if (world.getBlock(x, y + 1, z) == Blocks.glass) {
                metadata = 1;
                world.setBlockMetadataWithNotify(x, y, z, metadata, 2);
                world.setBlock(x, y + 1, z, ModBlocks.invisibleMultiblock, 2, 2);
            } else if (world.getBlock(x, y - 1, z) == Blocks.glass) {
                metadata = 2;
                world.setBlockMetadataWithNotify(x, y, z, metadata, 2);
                world.setBlock(x, y - 1, z, ModBlocks.invisibleMultiblock, 2, 2);
            }
        } else {
            TileEntity tile = world.getTileEntity(x, y, z);
            if (!(tile instanceof TileEnergyPylon) || (metadata == 1 && isGlassMissing(world, x, y + 1, z))
                    || (metadata == 2 && isGlassMissing(world, x, y - 1, z))) {
                metadata = 0;
                world.setBlockMetadataWithNotify(x, y, z, metadata, 2);
            }
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileEnergyPylon) {
            ((TileEnergyPylon) tile).onActivated();
            if (metadata == 0) {
                world.removeTileEntity(x, y, z);
            }
        }
    }

    private boolean isGlassMissing(World world, int x, int y, int z) {
        return world.getBlock(x, y, z) != ModBlocks.invisibleMultiblock || world.getBlockMetadata(x, y, z) != 2;
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float subX,
            float subY, float subZ) {
        int metadata = world.getBlockMetadata(x, y, z);
        if (metadata == 0) return false;
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileEnergyPylon pylon) {
            if (player.isSneaking()) {
                pylon.nextCore();
            } else {
                pylon.onActivated();
            }
            return true;
        }
        return false;
    }

    @Override
    public void onPostBlockPlaced(World world, int x, int y, int z, int metadata) {
        updateBlockState(world, x, y, z);
    }

    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride(World world, int x, int y, int z, int side) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileEnergyPylon pylon) {
            return (int) (pylon.getEnergyStored() / pylon.getMaxEnergyStored() * 15D);
        }
        return 0;
    }
}
