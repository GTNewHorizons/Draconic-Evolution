package com.brandon3055.draconicevolution.common.blocks;

import net.minecraft.block.ITileEntityProvider;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.common.ModBlocks;
import com.brandon3055.draconicevolution.common.ModItems;
import com.brandon3055.draconicevolution.common.blocks.itemblocks.CKeyStoneItemBlock;
import com.brandon3055.draconicevolution.common.lib.References;
import com.brandon3055.draconicevolution.common.lib.Strings;
import com.brandon3055.draconicevolution.common.tileentities.TileCKeyStone;
import com.brandon3055.draconicevolution.common.utils.ItemNBTHelper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Created by Brandon on 27/08/2014.
 */
public class CKeyStone extends BlockDE implements ITileEntityProvider {

    @SideOnly(Side.CLIENT)
    private IIcon blockIconActive;

    public CKeyStone() {
        this.setBlockUnbreakable();
        this.setCreativeTab(DraconicEvolution.tabBlocksItems);
        this.setBlockName(Strings.cKeyStoneName);
        ModBlocks.register(this, CKeyStoneItemBlock.class);
    }

    @Override
    public boolean hasTileEntity(int metadata) {
        return true;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new TileCKeyStone();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister iconRegister) {
        blockIcon = iconRegister.registerIcon(References.RESOURCESPREFIX + "key_stone_inactive");
        blockIconActive = iconRegister.registerIcon(References.RESOURCESPREFIX + "key_stone_active");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        return side == 0 || side == 1 ? Blocks.furnace.getIcon(side, metadata) : blockIconActive;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
        if (side == 0 || side == 1) {
            return Blocks.furnace.getIcon(side, 0);
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileCKeyStone keyStone) {
            return keyStone.isActivated ? blockIconActive : blockIcon;
        }
        return blockIcon;
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float subX,
            float subY, float subZ) {
        TileEntity tile = world.getTileEntity(x, y, z);
        return tile instanceof TileCKeyStone keyStone && keyStone.onActivated(player.getHeldItem(), player);
    }

    @Override
    public boolean canConnectRedstone(IBlockAccess world, int x, int y, int z, int side) {
        return true;
    }

    @Override
    public boolean canProvidePower() {
        return true;
    }

    @Override
    public int isProvidingWeakPower(IBlockAccess world, int x, int y, int z, int meta) {
        TileEntity tile = world.getTileEntity(x, y, z);
        return tile instanceof TileCKeyStone keyStone && keyStone.isActivated ? 15 : 0;
    }

    @Override
    public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z, EntityPlayer player) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileCKeyStone keyStone) {
            ItemStack key = new ItemStack(ModItems.key);
            ItemNBTHelper.setInteger(key, "KeyCode", keyStone.getKeyCode());
            ItemNBTHelper.setInteger(key, "X", x);
            ItemNBTHelper.setInteger(key, "Y", y);
            ItemNBTHelper.setInteger(key, "Z", z);
            return key;
        }
        return null;
    }

    @Override
    public boolean isBlockSolid(IBlockAccess world, int x, int y, int z, int side) {
        return true;
    }

    @Override
    public boolean isSideSolid(IBlockAccess world, int x, int y, int z, ForgeDirection side) {
        return true;
    }
}
