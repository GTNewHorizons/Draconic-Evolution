package com.brandon3055.draconicevolution.common.blocks.multiblock;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.common.ModBlocks;
import com.brandon3055.draconicevolution.common.blocks.BlockDE;
import com.brandon3055.draconicevolution.common.lib.References;
import com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.reactor.TileReactorCore;
import com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.reactor.TileReactorStabilizer;
import com.brandon3055.draconicevolution.common.utils.Utils;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Created by Brandon on 5/7/2015.
 */
public class ReactorStabilizer extends BlockDE implements ITileEntityProvider {

    public ReactorStabilizer() {
        this.setCreativeTab(DraconicEvolution.tabBlocksItems);
        this.setBlockName("reactorStabilizer");
        ModBlocks.register(this);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister iconRegister) {
        blockIcon = iconRegister.registerIcon(References.RESOURCESPREFIX + "transparency");
    }

    @Override
    public int getRenderType() {
        return -1;
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float subX,
            float subY, float subZ) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof IReactorPart part) {
            if (player.isSneaking()) {
                part.changeComparatorMode();
                if (!world.isRemote) {
                    player.addChatComponentMessage(new ChatComponentText(part.getComparatorMode().toLocalizedString()));
                }
                return true;
            }
            TileReactorCore core = part.getMaster();
            if (core != null) {
                return core.onStructureRightClicked(player);
            }
        }
        return false;
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
    public boolean hasTileEntity(int metadata) {
        return true;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new TileReactorStabilizer();
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase entity, ItemStack stack) {
        ForgeDirection facing = ForgeDirection.getOrientation(Utils.determineOrientation(x, y, z, entity));
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileReactorStabilizer stabilizer) {
            stabilizer.facing = entity.isSneaking() ? facing.getOpposite() : facing;
            stabilizer.onPlaced();
        }
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block blockBroken, int metadata) {
        TileEntity tile = world.getTileEntity(x, y, z);
        TileReactorCore core = tile instanceof IReactorPart part ? part.getMaster() : null;
        super.breakBlock(world, x, y, z, blockBroken, metadata);
        if (core != null) {
            core.updateReactorParts(false);
            core.validateStructure();
        }
    }

    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride(World world, int x, int y, int z, int side) {
        return IReactorPart.getComparatorOutput(world, x, y, z);
    }
}
