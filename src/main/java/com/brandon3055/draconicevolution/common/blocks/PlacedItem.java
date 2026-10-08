package com.brandon3055.draconicevolution.common.blocks;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import com.brandon3055.draconicevolution.common.ModBlocks;
import com.brandon3055.draconicevolution.common.lib.Strings;
import com.brandon3055.draconicevolution.common.tileentities.TilePlacedItem;

/**
 * Created by Brandon on 13/08/2014.
 */
public class PlacedItem extends BlockDE {

    public PlacedItem() {
        super(Material.circuits);
        this.setHardness(5F);
        this.setResistance(20F);
        this.setBlockName(Strings.placedItemName);
        ModBlocks.register(this);
    }

    @Override
    public void registerBlockIcons(IIconRegister iconRegister) {
        blockIcon = iconRegister.registerIcon("glass");
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        switch (world.getBlockMetadata(x, y, z)) {
            case 0:
                setBlockBounds(0.0F, 0.8F, 0.0F, 1.0F, 1.0F, 1.0F);
                break;
            case 1:
                setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.2F, 1.0F);
                break;
            case 2:
                setBlockBounds(0.0F, 0.0F, 0.8F, 1.0F, 1.0F, 1.0F);
                break;
            case 3:
                setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0.2F);
                break;
            case 4:
                setBlockBounds(0.8F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
                break;
            case 5:
                setBlockBounds(0.0F, 0.0F, 0.0F, 0.2F, 1.0F, 1.0F);
                break;
            default:
                super.setBlockBoundsBasedOnState(world, x, y, z);
        }
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        TilePlacedItem tile = (world.getTileEntity(x, y, z) != null
                && world.getTileEntity(x, y, z) instanceof TilePlacedItem)
                        ? (TilePlacedItem) world.getTileEntity(x, y, z)
                        : null;
        int meta = world.getBlockMetadata(x, y, z);
        if (tile != null && tile.getStack() != null) {
            if (tile.getStack().getItem() instanceof ItemBlock) {
                switch (meta) {
                    case 0:
                        return AxisAlignedBB
                                .getBoundingBox(x + 0.25D, y + 0.5D, z + 0.25D, x + 0.75D, y + 1D, z + 0.75D);
                    case 1:
                        return AxisAlignedBB
                                .getBoundingBox(x + 0.25D, y + 0D, z + 0.25D, x + 0.75D, y + 0.5D, z + 0.75D);
                    case 2:
                        return AxisAlignedBB
                                .getBoundingBox(x + 0.25D, y + 0.25D, z + 0.5D, x + 0.75D, y + 0.75D, z + 1D);
                    case 3:
                        return AxisAlignedBB
                                .getBoundingBox(x + 0.25D, y + 0.25D, z + 0D, x + 0.75D, y + 0.75D, z + 0.5D);
                    case 4:
                        return AxisAlignedBB
                                .getBoundingBox(x + 0.5D, y + 0.25D, z + 0.25D, x + 1D, y + 0.75D, z + 0.75D);
                    case 5:
                        return AxisAlignedBB
                                .getBoundingBox(x + 0D, y + 0.25D, z + 0.25D, x + 0.5D, y + 0.75D, z + 0.75D);
                }
            } else {
                switch (meta) {
                    case 0:
                        return AxisAlignedBB
                                .getBoundingBox(x + 0.25D, y + 0.9D, z + 0.25D, x + 0.75D, y + 1D, z + 0.75D);
                    case 1:
                        return AxisAlignedBB
                                .getBoundingBox(x + 0.25D, y + 0D, z + 0.25D, x + 0.75D, y + 0.1D, z + 0.75D);
                    case 2:
                        return AxisAlignedBB
                                .getBoundingBox(x + 0.25D, y + 0.25D, z + 0.9D, x + 0.75D, y + 0.75D, z + 1D);
                    case 3:
                        return AxisAlignedBB
                                .getBoundingBox(x + 0.25D, y + 0.25D, z + 0D, x + 0.75D, y + 0.75D, z + 0.1D);
                    case 4:
                        return AxisAlignedBB
                                .getBoundingBox(x + 0.9D, y + 0.25D, z + 0.25D, x + 1D, y + 0.75D, z + 0.75D);
                    case 5:
                        return AxisAlignedBB
                                .getBoundingBox(x + 0D, y + 0.25D, z + 0.25D, x + 0.1D, y + 0.75D, z + 0.75D);
                }
            }
        }
        return super.getCollisionBoundingBoxFromPool(world, x, y, z);
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
    public TileEntity createTileEntity(World world, int metadata) {
        return new TilePlacedItem();
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        TileEntity te = world.getTileEntity(x, y, z);
        if (te instanceof TilePlacedItem tile) {
            ItemStack stack;
            while ((stack = tile.removeLastItem()) != null) {
                dropStack(world, x, y, z, stack);
            }
        }
        super.breakBlock(world, x, y, z, block, meta);
    }

    private static void dropStack(World world, int x, int y, int z, ItemStack stack) {
        float spawnX = x + world.rand.nextFloat();
        float spawnY = y + world.rand.nextFloat();
        float spawnZ = z + world.rand.nextFloat();

        EntityItem droppedItem = new EntityItem(world, spawnX, spawnY, spawnZ, stack);

        float multiplier = 0.05F;

        droppedItem.motionX = (-0.5F + world.rand.nextFloat()) * multiplier;
        droppedItem.motionY = (4 + world.rand.nextFloat()) * multiplier;
        droppedItem.motionZ = (-0.5F + world.rand.nextFloat()) * multiplier;

        world.spawnEntityInWorld(droppedItem);
    }

    @Override
    public Item getItemDropped(int p_149650_1_, Random p_149650_2_, int p_149650_3_) {
        return null;
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
            float hitY, float hitZ) {
        TilePlacedItem tile = (world.getTileEntity(x, y, z) != null
                && world.getTileEntity(x, y, z) instanceof TilePlacedItem)
                        ? (TilePlacedItem) world.getTileEntity(x, y, z)
                        : null;
        if (tile == null) {
            world.setBlockToAir(x, y, z);
            return true;
        }
        if (player.isSneaking()) {
            tile.rotation += 5.625F;
        } else {
            // Remove the item the player clicked on; the block is removed once the last one is taken.
            if (!world.isRemote) {
                int index = tile.getStackIndexAt(world.getBlockMetadata(x, y, z), hitX, hitY, hitZ);
                ItemStack stack = tile.removeItem(index);
                if (stack != null) {
                    dropStack(world, x, y, z, stack);
                }
                if (tile.getDisplayCount() == 0) {
                    world.setBlockToAir(x, y, z);
                }
            }
        }
        world.markBlockForUpdate(x, y, z);
        return true;
    }

    @Override
    public void onBlockClicked(World world, int x, int y, int z, EntityPlayer player) {
        TilePlacedItem tile = (world.getTileEntity(x, y, z) != null
                && world.getTileEntity(x, y, z) instanceof TilePlacedItem)
                        ? (TilePlacedItem) world.getTileEntity(x, y, z)
                        : null;
        if (tile == null) {
            world.setBlockToAir(x, y, z);
            return;
        }
        tile.rotation += player.isSneaking() ? 22.5F : -22.5F;
        world.markBlockForUpdate(x, y, z);
    }

    @Override
    public int getLightValue(IBlockAccess world, int x, int y, int z) {
        TileEntity te = world.getTileEntity(x, y, z);
        int light = 0;
        if (te instanceof TilePlacedItem tile) {
            for (ItemStack stack : tile.getStacks()) {
                if (stack != null && stack.getItem() instanceof ItemBlock) {
                    light = Math.max(light, Block.getBlockFromItem(stack.getItem()).getLightValue());
                }
            }
        }
        return light;
    }

    @Override
    public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z) {
        TileEntity te = world.getTileEntity(x, y, z);
        if (te instanceof TilePlacedItem tile && target != null && target.hitVec != null) {
            int index = tile.getStackIndexAt(
                    world.getBlockMetadata(x, y, z),
                    (float) (target.hitVec.xCoord - x),
                    (float) (target.hitVec.yCoord - y),
                    (float) (target.hitVec.zCoord - z));
            return tile.getStack(index);
        }
        return null;
    }
}
