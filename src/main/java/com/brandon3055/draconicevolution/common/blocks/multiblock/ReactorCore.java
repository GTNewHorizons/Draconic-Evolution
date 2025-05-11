package com.brandon3055.draconicevolution.common.blocks.multiblock;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.common.ModBlocks;
import com.brandon3055.draconicevolution.common.blocks.BlockCustomDrop;
import com.brandon3055.draconicevolution.common.lib.References;
import com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.reactor.TileReactorCore;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Created by Brandon on 16/6/2015.
 */
public class ReactorCore extends BlockCustomDrop implements ITileEntityProvider {

    public ReactorCore() {
        super(Material.iron);
        this.setCreativeTab(DraconicEvolution.tabBlocksItems);
        this.setBlockName("reactorCore");

        ModBlocks.register(this);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister iconRegister) {
        blockIcon = iconRegister.registerIcon(References.RESOURCESPREFIX + "transparency");
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public int getRenderType() {
        return -1;
    }

    @Override
    public boolean hasTileEntity(int metadata) {
        return true;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new TileReactorCore();
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileReactorCore core) {
            core.onPlaced();
        }
    }

    @Override
    public AxisAlignedBB getSelectedBoundingBoxFromPool(World world, int x, int y, int z) {
        return AxisAlignedBB.getBoundingBox(0, 0, 0, 1, 1, 1);
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block blockBroken, int metadata) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileReactorCore core) {
            core.onBroken();
        }
        super.breakBlock(world, x, y, z, blockBroken, metadata);
    }

    @Override
    protected boolean dropInventory() {
        return true;
    }

    @Override
    protected boolean hasCustomDropps() {
        return false;
    }

    @Override
    protected void getCustomTileEntityDrops(TileEntity te, List<ItemStack> droppes) {

    }
}
