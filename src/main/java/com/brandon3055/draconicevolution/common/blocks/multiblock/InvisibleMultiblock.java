package com.brandon3055.draconicevolution.common.blocks.multiblock;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import com.brandon3055.draconicevolution.common.ModBlocks;
import com.brandon3055.draconicevolution.common.blocks.BlockDE;
import com.brandon3055.draconicevolution.common.handler.BalanceConfigHandler;
import com.brandon3055.draconicevolution.common.lib.References;
import com.brandon3055.draconicevolution.common.lib.Strings;
import com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.TileEnergyPylon;
import com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.TileEnergyStorageCore;
import com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.TileInvisibleMultiblock;
import com.brandon3055.draconicevolution.common.utils.IHudDisplayBlock;
import com.brandon3055.draconicevolution.common.utils.LogHelper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Created by Brandon on 25/07/2014.
 */
public class InvisibleMultiblock extends BlockDE implements IHudDisplayBlock, ITileEntityProvider {

    public static void revertStructure(World world, int x, int y, int z) {
        int metadata = world.getBlockMetadata(x, y, z);
        if (metadata == 0) {
            world.setBlock(
                    x,
                    y,
                    z,
                    BalanceConfigHandler.energyStorageStructureOuterBlock,
                    BalanceConfigHandler.energyStorageStructureOuterBlockMetadata,
                    3);
        } else if (metadata == 1) {
            world.setBlock(
                    x,
                    y,
                    z,
                    BalanceConfigHandler.energyStorageStructureBlock,
                    BalanceConfigHandler.energyStorageStructureBlockMetadata,
                    3);
        }
    }

    public InvisibleMultiblock() {
        super(Material.iron);
        this.setHardness(10F);
        this.setResistance(2000F);
        this.setBlockName(Strings.invisibleMultiblockName);
        ModBlocks.register(this);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister iconRegister) {
        blockIcon = iconRegister.registerIcon(References.RESOURCESPREFIX + "draconium_block_0");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderType() {
        return -1;
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
        return metadata == 0 || metadata == 1;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return metadata == 0 || metadata == 1 ? new TileInvisibleMultiblock() : null;
    }

    @Override
    public Item getItemDropped(int metadata, Random random, int fortune) {
        if (metadata == 0) {
            return Item.getItemFromBlock(BalanceConfigHandler.energyStorageStructureOuterBlock);
        }
        if (metadata == 1) {
            return Item.getItemFromBlock(BalanceConfigHandler.energyStorageStructureBlock);
        }
        return null;
    }

    @Override
    public int damageDropped(int metadata) {
        if (metadata == 0) {
            return BalanceConfigHandler.energyStorageStructureOuterBlockMetadata;
        }
        if (metadata == 1) {
            return BalanceConfigHandler.energyStorageStructureBlockMetadata;
        }
        return super.damageDropped(metadata);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float subX,
            float subY, float subZ) {
        int metadata = world.getBlockMetadata(x, y, z);
        if (metadata == 0 || metadata == 1) {
            TileEntity tile = world.getTileEntity(x, y, z);
            if (!(tile instanceof TileInvisibleMultiblock multiblock)) {
                LogHelper.error("Missing Tile Entity (TileInvisibleMultiblock)");
                return false;
            }
            TileEnergyStorageCore core = multiblock.getMaster();
            if (core == null) {
                onNeighborBlockChange(world, x, y, z, this);
                return false;
            }
            if (!world.isRemote) {
                world.markBlockForUpdate(core.xCoord, core.yCoord, core.zCoord);
                List<String> information = core.getDisplayInformation(false);
                for (String message : information) {
                    player.addChatComponentMessage(new ChatComponentText(message));
                }
            }
            return true;
        } else if (metadata == 2) {
            TileEntity tileAbove = world.getTileEntity(x, y + 1, z);
            TileEntity tileBelow = world.getTileEntity(x, y - 1, z);
            TileEnergyPylon pylon = tileAbove instanceof TileEnergyPylon ? (TileEnergyPylon) tileAbove
                    : tileBelow instanceof TileEnergyPylon ? (TileEnergyPylon) tileBelow : null;
            if (pylon == null) {
                return false;
            }
            pylon.isReceivingEnergy = !pylon.isReceivingEnergy;
            world.markBlockForUpdate(pylon.xCoord, pylon.yCoord, pylon.zCoord);
            pylon.onActivated();
            return true;
        }
        return false;
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        int metadata = world.getBlockMetadata(x, y, z);
        if (metadata == 0 || metadata == 1) {
            TileEntity tile = world.getTileEntity(x, y, z);
            if (!(tile instanceof TileInvisibleMultiblock multiblock)) {
                LogHelper.error("Missing Tile Entity (TileInvisibleMultiblock)");
                revertStructure(world, x, y, z);
                return;
            }
            TileEnergyStorageCore core = multiblock.getMaster();
            if (core == null) {
                LogHelper.error("Master = null reverting!");
                revertStructure(world, x, y, z);
                return;
            }
            if (core.isOnline()) {
                core.validateStructure(core.getTier() == 1);
            } else {
                revertStructure(world, x, y, z);
            }
        } else if (metadata == 2) {
            if (world.getBlock(x, y + 1, z) != ModBlocks.energyPylon
                    && world.getBlock(x, y - 1, z) != ModBlocks.energyPylon)
                world.setBlock(x, y, z, Blocks.glass);
        }
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block blockBroken, int metadata) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileInvisibleMultiblock multiblock) {
            TileEnergyStorageCore core = multiblock.getMaster();
            if (core != null && core.isOnline()) {
                world.setBlockMetadataWithNotify(x, y, z, 0, 2);
                core.validateStructure(core.getTier() == 1);
            }
        }
        super.breakBlock(world, x, y, z, blockBroken, metadata);
    }

    @Override
    public AxisAlignedBB getSelectedBoundingBoxFromPool(World world, int x, int y, int z) {
        int metadata = world.getBlockMetadata(x, y, z);
        if (metadata == 0 || metadata == 1) {
            TileEntity tile = world.getTileEntity(x, y, z);
            if (tile instanceof TileInvisibleMultiblock multiblock) {
                TileEnergyStorageCore core = multiblock.getMaster();
                if (core != null) {
                    return AxisAlignedBB.getBoundingBox(
                            core.xCoord,
                            core.yCoord,
                            core.zCoord,
                            core.xCoord + 0.5,
                            core.yCoord + 0.5,
                            core.zCoord + 0.5);
                }
            }
            return super.getSelectedBoundingBoxFromPool(world, x, y, z);
        } else if (metadata == 2) {
            return AxisAlignedBB.getBoundingBox(x + 0.49, y + 0.49, z + 0.49, x + 0.51, y + 0.51, z + 0.51);
        }
        return super.getSelectedBoundingBoxFromPool(world, x, y, z);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        int metadata = world.getBlockMetadata(x, y, z);
        if (metadata == 2) {
            return AxisAlignedBB.getBoundingBox(x, y, z, x, y, z);
        }
        return super.getCollisionBoundingBoxFromPool(world, x, y, z);
    }

    @Override
    public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z, EntityPlayer player) {
        int metadata = world.getBlockMetadata(x, y, z);
        if (metadata == 0) {
            return new ItemStack(
                    BalanceConfigHandler.energyStorageStructureOuterBlock,
                    1,
                    BalanceConfigHandler.energyStorageStructureOuterBlockMetadata);
        }
        if (metadata == 1) {
            return new ItemStack(
                    BalanceConfigHandler.energyStorageStructureBlock,
                    1,
                    BalanceConfigHandler.energyStorageStructureBlockMetadata);
        }
        return new ItemStack(Blocks.glass);
    }

    @Override
    public List<String> getDisplayData(World world, int x, int y, int z) {
        int metadata = world.getBlockMetadata(x, y, z);
        if (metadata == 0 || metadata == 1) {
            TileEntity tile = world.getTileEntity(x, y, z);
            if (!(tile instanceof TileInvisibleMultiblock multiblock)) {
                LogHelper.error("Missing Tile Entity (TileInvisibleMultiblock getDisplayData)");
                return Collections.emptyList();
            }
            TileEnergyStorageCore core = multiblock.getMaster();
            if (core != null) {
                return core.getDisplayInformation(true);
            }
        }
        return Collections.emptyList();
    }
}
