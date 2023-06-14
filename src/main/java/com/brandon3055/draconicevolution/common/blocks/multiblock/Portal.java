package com.brandon3055.draconicevolution.common.blocks.multiblock;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import com.brandon3055.draconicevolution.common.ModBlocks;
import com.brandon3055.draconicevolution.common.blocks.BlockDE;
import com.brandon3055.draconicevolution.common.lib.References;
import com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.TileDislocatorReceptacle;
import com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.TilePortalBlock;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Created by Brandon on 23/5/2015.
 */
public class Portal extends BlockDE implements ITileEntityProvider {

    public Portal() {
        super(Material.portal);
        this.setBlockUnbreakable();
        this.setBlockName("portal");

        ModBlocks.register(this);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister iconRegister) {
        blockIcon = iconRegister.registerIcon(References.RESOURCESPREFIX + "transparency");
    }

    @Override
    public int getRenderType() {
        return References.idPortal;
    }

    @Override
    public AxisAlignedBB getSelectedBoundingBoxFromPool(World worldIn, int x, int y, int z) {
        return AxisAlignedBB.getBoundingBox(0, 0, 0, 0, 0, 0);
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
    public boolean hasTileEntity(int metadata) {
        return true;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new TilePortalBlock();
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World worldIn, int x, int y, int z) {
        return null;
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block block) {
        if (world.isRemote) {
            return;
        }
        TileDislocatorReceptacle receptacle = getMaster(world, x, y, z);
        if (receptacle == null) {
            world.setBlockToAir(x, y, z);
            return;
        }

        if (receptacle.isActive) {
            receptacle.validateActivePortal();
        }
        if (!receptacle.isActive && !receptacle.updating) {
            world.setBlockToAir(x, y, z);
            return;
        }
        updateMetadata(world, x, y, z);
    }

    @Override
    public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
        if (world.isRemote) {
            return;
        }
        TileDislocatorReceptacle receptacle = getMaster(world, x, y, z);
        if (receptacle == null) {
            world.setBlockToAir(x, y, z);
            return;
        }
        if (receptacle.isActive && receptacle.getLocation() != null) {
            if (receptacle.coolDown > 0) {
                return;
            }
            receptacle.coolDown = 1;
            receptacle.getLocation().sendEntityToCoords(entity);
        } else {
            receptacle.validateActivePortal();
        }
    }

    private TileDislocatorReceptacle getMaster(World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        return tile instanceof TilePortalBlock portal ? portal.getMaster() : null;
    }

    @Override
    public Item getItemDropped(int metadata, Random random, int fortune) {
        return null;
    }

    private boolean isPortalOrFrame(IBlockAccess world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        return block == ModBlocks.portal || block == ModBlocks.infusedObsidian
                || block == ModBlocks.dislocatorReceptacle;
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        updateMetadata(world, x, y, z);
    }

    private void updateMetadata(World world, int x, int y, int z) {
        if (world.isRemote || world.getBlockMetadata(x, y, z) != 0) {
            return;
        }
        int metadata = 0;
        boolean hasPortalsAlongXAxis = isPortalOrFrame(world, x + 1, y, z) && isPortalOrFrame(world, x - 1, y, z);
        boolean hasPortalsAlongYAxis = isPortalOrFrame(world, x, y + 1, z) && isPortalOrFrame(world, x, y - 1, z);
        boolean hasPortalsAlongZAxis = isPortalOrFrame(world, x, y, z + 1) && isPortalOrFrame(world, x, y, z - 1);

        if (hasPortalsAlongXAxis && hasPortalsAlongYAxis) {
            metadata = 1;
        } else if (hasPortalsAlongYAxis && hasPortalsAlongZAxis) {
            metadata = 2;
        } else if (hasPortalsAlongXAxis && hasPortalsAlongZAxis) {
            metadata = 3;
        }
        world.setBlockMetadataWithNotify(x, y, z, metadata, 2);
    }
}
