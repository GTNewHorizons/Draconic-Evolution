package com.brandon3055.draconicevolution.common.blocks.multiblock;

import net.minecraft.block.ITileEntityProvider;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.common.ModBlocks;
import com.brandon3055.draconicevolution.common.blocks.BlockDE;
import com.brandon3055.draconicevolution.common.lib.References;
import com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.reactor.TileReactorEnergyInjector;
import com.brandon3055.draconicevolution.common.utils.Utils;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Created by Brandon on 23/7/2015.
 */
public class ReactorEnergyInjector extends BlockDE implements ITileEntityProvider {

    public ReactorEnergyInjector() {
        this.setCreativeTab(DraconicEvolution.tabBlocksItems);
        this.setBlockName("reactorEnergyInjector");

        ModBlocks.register(this);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister iconRegister) {
        blockIcon = iconRegister.registerIcon(References.RESOURCESPREFIX + "transparency");
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        initializeBoundingBox(world, x, y, z);
        super.setBlockBoundsBasedOnState(world, x, y, z);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        initializeBoundingBox(world, x, y, z);
        return super.getCollisionBoundingBoxFromPool(world, x, y, z);
    }

    private void initializeBoundingBox(IBlockAccess world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileReactorEnergyInjector injector) {
            switch (injector.facing) {
                case DOWN -> this.setBlockBounds(0F, 0.875F, 0F, 1F, 1F, 1F);
                case UP -> this.setBlockBounds(0F, 0F, 0F, 1F, 0.125F, 1F);
                case NORTH -> this.setBlockBounds(0F, 0F, 0.875F, 1F, 1F, 1F);
                case SOUTH -> this.setBlockBounds(0F, 0F, 0F, 1F, 1F, 0.125F);
                case WEST -> this.setBlockBounds(0.875F, 0F, 0F, 1F, 1F, 1F);
                case EAST -> this.setBlockBounds(0F, 0F, 0F, 0.125F, 1F, 1F);
            }
        }
    }

    @Override
    public int getRenderType() {
        return -1;
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
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase entity, ItemStack stack) {
        ForgeDirection facing = ForgeDirection.getOrientation(Utils.determineOrientation(x, y, z, entity));
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileReactorEnergyInjector injector) {
            injector.facing = entity.isSneaking() ? facing.getOpposite() : facing;
            injector.onPlaced();
        }
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float subX,
            float subY, float subZ) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof IReactorPart part && player.isSneaking()) {
            part.changeComparatorMode();
            if (!world.isRemote) {
                player.addChatComponentMessage(new ChatComponentText(part.getComparatorMode().toLocalizedString()));
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride(World world, int x, int y, int z, int side) {
        return IReactorPart.getComparatorOutput(world, x, y, z);
    }

    @Override
    public boolean hasTileEntity(int metadata) {
        return true;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new TileReactorEnergyInjector();
    }
}
