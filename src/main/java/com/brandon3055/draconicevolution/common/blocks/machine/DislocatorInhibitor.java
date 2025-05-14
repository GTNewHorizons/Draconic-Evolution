package com.brandon3055.draconicevolution.common.blocks.machine;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.client.gui.GuiHandler;
import com.brandon3055.draconicevolution.common.ModBlocks;
import com.brandon3055.draconicevolution.common.blocks.BlockDE;
import com.brandon3055.draconicevolution.common.lib.References;
import com.brandon3055.draconicevolution.common.lib.Strings;
import com.brandon3055.draconicevolution.common.tileentities.TileDislocatorInhibitor;

import cpw.mods.fml.common.network.internal.FMLNetworkHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class DislocatorInhibitor extends BlockDE implements ITileEntityProvider {

    public DislocatorInhibitor() {
        this.setBlockName(Strings.dislocatorInhibitor);
        this.setCreativeTab(DraconicEvolution.tabBlocksItems);
        this.setStepSound(soundTypeStone);
        ModBlocks.register(this, DislocatorInhibitorItemBlock.class);
    }

    @Override
    public boolean isBlockSolid(IBlockAccess world, int x, int y, int z, int side) {
        return true;
    }

    @Override
    public boolean isSideSolid(IBlockAccess world, int x, int y, int z, ForgeDirection side) {
        return true;
    }

    @Override
    public boolean hasTileEntity(int metadata) {
        return true;
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileDislocatorInhibitor();
    }

    @Override
    public boolean canConnectRedstone(IBlockAccess world, int x, int y, int z, int side) {
        return true;
    }

    private void updateRedstoneStatus(World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile != null) {
            if (tile instanceof TileDislocatorInhibitor te) {
                if (te.isRedstoneActive() ^ world.isBlockIndirectlyGettingPowered(x, y, z)) {
                    te.setRedstoneActive(world.isBlockIndirectlyGettingPowered(x, y, z));
                    world.markBlockForUpdate(x, y, z);
                }
            }
        }
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        updateRedstoneStatus(world, x, y, z);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float prx,
            float pry, float prz) {

        if (!world.isRemote) {
            FMLNetworkHandler
                    .openGui(player, DraconicEvolution.instance, GuiHandler.GUIID_DISLOCATOR_INHIBITOR, world, x, y, z);
        }
        return true;
    }

    @Override
    public void onPostBlockPlaced(World world, int x, int y, int z, int meta) {
        updateRedstoneStatus(world, x, y, z);
    }

    @Override
    public void breakBlock(World worldIn, int x, int y, int z, Block blockBroken, int meta) {
        TileEntity tile = worldIn.getTileEntity(x, y, z);
        if (tile != null) {
            if (tile instanceof TileDislocatorInhibitor) {
                ((TileDislocatorInhibitor) tile).unregister();
            }
        }

        super.breakBlock(worldIn, x, y, z, blockBroken, meta);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerBlockIcons(IIconRegister iconRegister) {
        blockIcon = iconRegister.registerIcon(References.RESOURCESPREFIX + "inhibitor_block");
    }

    public static class DislocatorInhibitorItemBlock extends ItemBlock {

        public DislocatorInhibitorItemBlock(Block block) {
            super(block);
        }

        @SideOnly(Side.CLIENT)
        @Override
        public void addInformation(ItemStack item, EntityPlayer player, List lines, boolean advanced) {
            lines.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("info.de.dislocatorInhibitor.txt"));
            super.addInformation(item, player, lines, advanced);
        }
    }
}
