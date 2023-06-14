package com.brandon3055.draconicevolution.common.blocks.machine;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.client.gui.GuiHandler;
import com.brandon3055.draconicevolution.common.ModBlocks;
import com.brandon3055.draconicevolution.common.blocks.BlockCustomDrop;
import com.brandon3055.draconicevolution.common.lib.References;
import com.brandon3055.draconicevolution.common.lib.Strings;
import com.brandon3055.draconicevolution.common.tileentities.TilePlayerDetectorAdvanced;

import cpw.mods.fml.common.network.internal.FMLNetworkHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class PlayerDetectorAdvanced extends BlockCustomDrop {

    @SideOnly(Side.CLIENT)
    IIcon side_inactive;
    @SideOnly(Side.CLIENT)
    IIcon side_active;
    @SideOnly(Side.CLIENT)
    IIcon top;
    @SideOnly(Side.CLIENT)
    IIcon bottom;

    public PlayerDetectorAdvanced() {
        super(Material.iron);
        this.setBlockName(Strings.playerDetectorAdvancedName);
        this.setCreativeTab(DraconicEvolution.tabBlocksItems);
        this.setStepSound(soundTypeStone);
        ModBlocks.register(this);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister iconRegister) {
        side_inactive = iconRegister
                .registerIcon(References.RESOURCESPREFIX + "advanced_player_detector_side_inactive");
        side_active = iconRegister.registerIcon(References.RESOURCESPREFIX + "advanced_player_detector_side_active");
        top = iconRegister.registerIcon(References.RESOURCESPREFIX + "machine_top_0");
        bottom = iconRegister.registerIcon(References.RESOURCESPREFIX + "machine_side");
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
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TilePlayerDetectorAdvanced detector) {
            if (detector.getStackInSlot(0) != null) {
                ItemStack stack = detector.getStackInSlot(0);
                Block block = Block.getBlockFromItem(stack.getItem());
                if (block != null && block.renderAsNormalBlock()) {
                    return block.getIcon(side, stack.getItemDamage());
                }
            } else {
                if (side == 0) {
                    return bottom;
                }
                if (side == 1) {
                    return top;
                }
                return detector.shouldOutput() ? side_active : side_inactive;
            }
        }
        return side_active;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        if (side == 0) {
            return bottom;
        }
        if (side == 1) {
            return top;
        }
        return side_active;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new TilePlayerDetectorAdvanced();
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float subX,
            float subY, float subZ) {
        if (!world.isRemote) {
            FMLNetworkHandler
                    .openGui(player, DraconicEvolution.instance, GuiHandler.GUIID_PLAYERDETECTOR, world, x, y, z);
        }
        return true;
    }

    @Override
    public boolean canConnectRedstone(IBlockAccess world, int x, int y, int z, int side) {
        return side >= 0;
    }

    @Override
    public boolean canProvidePower() {
        return true;
    }

    @Override
    public int isProvidingWeakPower(IBlockAccess world, int x, int y, int z, int side) {
        return isProvidingStrongPower(world, x, y, z, side);
    }

    @Override
    public int isProvidingStrongPower(IBlockAccess world, int x, int y, int z, int side) {
        TileEntity tile = world.getTileEntity(x, y, z);
        return tile instanceof TilePlayerDetectorAdvanced detector && detector.shouldOutput() ? 15 : 0;
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
    protected void getCustomTileEntityDrops(TileEntity tile, List<ItemStack> drops) {}

    @Override
    public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z, EntityPlayer player) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TilePlayerDetectorAdvanced detector) {
            ItemStack stackInSlot = detector.getStackInSlot(0);
            if (stackInSlot != null) {
                return stackInSlot;
            }
        }
        return super.getPickBlock(target, world, x, y, z, player);
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block blockBroken, int metadata) {
        super.breakBlock(world, x, y, z, blockBroken, metadata);
        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            world.notifyBlocksOfNeighborChange(
                    x + direction.offsetX,
                    y + direction.offsetY,
                    z + direction.offsetZ,
                    blockBroken,
                    direction.getOpposite().ordinal());
        }
    }
}
