package com.brandon3055.draconicevolution.common.blocks.multiblock;

import java.util.List;
import java.util.Random;

import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.common.ModBlocks;
import com.brandon3055.draconicevolution.common.blocks.BlockCustomDrop;
import com.brandon3055.draconicevolution.common.items.tools.TeleporterMKI;
import com.brandon3055.draconicevolution.common.lib.References;
import com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.TileDislocatorReceptacle;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Created by Brandon on 19/5/2015.
 */
public class DislocatorReceptacle extends BlockCustomDrop implements ITileEntityProvider {

    @SideOnly(Side.CLIENT)
    IIcon textureInactive;

    public DislocatorReceptacle() {
        super(Material.rock);
        this.setHardness(50.0F);
        this.setResistance(2000.0F);
        this.setBlockName("dislocatorReceptacle");
        this.setHarvestLevel("pickaxe", 3);
        this.setCreativeTab(DraconicEvolution.tabBlocksItems);

        ModBlocks.register(this);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister iconRegister) {
        textureInactive = iconRegister
                .registerIcon(References.RESOURCESPREFIX + "animated/dislocatorReceptacle_inactive");
        blockIcon = iconRegister.registerIcon(References.RESOURCESPREFIX + "animated/dislocatorReceptacle_active");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileDislocatorReceptacle receptacle) {
            return receptacle.isActive ? blockIcon : textureInactive;
        }
        return blockIcon;
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileDislocatorReceptacle receptacle) {
            receptacle.updateState();
        }
    }

    @Override
    public boolean hasTileEntity(int metadata) {
        return true;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new TileDislocatorReceptacle();
    }

    @Override
    public boolean canEntityDestroy(IBlockAccess world, int x, int y, int z, Entity entity) {
        return false;
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float subX,
            float subY, float subZ) {
        if (world.isRemote) {
            return true;
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof TileDislocatorReceptacle receptacle)) {
            return false;
        }
        ItemStack stackInSlot = receptacle.getStackInSlot(0);
        if (stackInSlot != null) {
            if (player.getHeldItem() == null) {
                player.inventory.setInventorySlotContents(player.inventory.currentItem, stackInSlot);
            } else {
                world.spawnEntityInWorld(new EntityItem(world, player.posX, player.posY, player.posZ, stackInSlot));
            }
            receptacle.setInventorySlotContents(0, null);
            world.markBlockForUpdate(x, y, z);
            world.notifyBlockChange(x, y, z, this);
        } else {
            ItemStack stack = player.getHeldItem();
            if (stack != null && stack.getItem() instanceof TeleporterMKI teleporter
                    && teleporter.getLocation(stack) != null) {
                receptacle.setInventorySlotContents(0, player.getHeldItem());
                player.destroyCurrentEquippedItem();
            }
        }
        return true;
    }

    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride(World world, int x, int y, int z, int p_149736_5_) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileDislocatorReceptacle receptacle) {
            if (receptacle.getStackInSlot(0) != null) {
                return 15;
            }
        }
        return 0;
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
}
