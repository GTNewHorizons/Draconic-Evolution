package com.brandon3055.draconicevolution.common.blocks.machine;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.common.ModBlocks;
import com.brandon3055.draconicevolution.common.blocks.BlockDE;
import com.brandon3055.draconicevolution.common.lib.References;
import com.brandon3055.draconicevolution.common.lib.Strings;
import com.brandon3055.draconicevolution.common.tileentities.TilePlayerDetector;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class PlayerDetector extends BlockDE implements ITileEntityProvider {

    @SideOnly(Side.CLIENT)
    IIcon side_inactive;
    @SideOnly(Side.CLIENT)
    IIcon side_active;
    @SideOnly(Side.CLIENT)
    IIcon top;
    @SideOnly(Side.CLIENT)
    IIcon bottom;

    public PlayerDetector() {
        this.setBlockName(Strings.playerDetectorName);
        this.setCreativeTab(DraconicEvolution.tabBlocksItems);
        this.setStepSound(soundTypeStone);
        ModBlocks.register(this);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister iconRegister) {
        side_inactive = iconRegister.registerIcon(References.RESOURCESPREFIX + "player_detector_side_inactive");
        side_active = iconRegister.registerIcon(References.RESOURCESPREFIX + "player_detector_side_active");
        top = iconRegister.registerIcon(References.RESOURCESPREFIX + "machine_top_0");
        bottom = iconRegister.registerIcon(References.RESOURCESPREFIX + "machine_side");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
        if (side == 0) {
            return bottom;
        }
        if (side == 1) {
            return top;
        }
        IIcon side_icon;
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TilePlayerDetector detector && detector.shouldOutput()) {
            side_icon = side_active;
        } else {
            side_icon = side_inactive;
        }
        return side_icon;
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
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new TilePlayerDetector();
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
        return tile instanceof TilePlayerDetector detector && detector.shouldOutput() ? 15 : 0;
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float subX,
            float subY, float subZ) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TilePlayerDetector detector) {
            int range = detector.getRange();

            if (player.isSneaking()) {
                range--;
            } else {
                range++;
            }

            detector.setRange(range);

            if (world.isRemote) {
                player.addChatMessage(
                        new ChatComponentTranslation("msg.range.txt")
                                .appendSibling(new ChatComponentText(" " + detector.getRange())));
            }
        }
        return true;
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
