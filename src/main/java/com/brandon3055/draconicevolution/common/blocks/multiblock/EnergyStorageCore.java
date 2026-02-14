package com.brandon3055.draconicevolution.common.blocks.multiblock;

import java.util.Collections;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
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
import com.brandon3055.draconicevolution.common.lib.Strings;
import com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.TileEnergyStorageCore;
import com.brandon3055.draconicevolution.common.utils.IHudDisplayBlock;
import com.brandon3055.draconicevolution.common.utils.LogHelper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Created by Brandon on 25/07/2014.
 */
public class EnergyStorageCore extends BlockDE implements IHudDisplayBlock, ITileEntityProvider {

    public EnergyStorageCore() {
        super(Material.iron);
        this.setHardness(10F);
        this.setResistance(20f);
        this.setCreativeTab(DraconicEvolution.tabBlocksItems);
        this.setBlockName(Strings.energyStorageCoreName);
        ModBlocks.register(this);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister iconRegister) {
        blockIcon = iconRegister.registerIcon(References.RESOURCESPREFIX + "energy_storage_core");
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
        return new TileEnergyStorageCore();
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float subX,
            float subY, float subZ) {
        if (!world.isRemote) {
            TileEntity tile = world.getTileEntity(x, y, z);
            if (!(tile instanceof TileEnergyStorageCore core)) {
                LogHelper.error("Missing Tile Entity (EnergyStorageCore)");
                return false;
            }
            List<String> information = core.getDisplayInformation(false);
            for (String message : information) {
                player.addChatComponentMessage(new ChatComponentText(message));
            }
        }
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
        ForgeDirection direction = ForgeDirection.getOrientation(side);
        TileEntity tile = world.getTileEntity(x - direction.offsetX, y - direction.offsetY, z - direction.offsetZ);
        if (!(tile instanceof TileEnergyStorageCore core)) {
            LogHelper.error("Missing Tile Entity (EnergyStorageCore)(shouldSideBeRendered)");
            return true;
        }
        return !core.isOnline() && super.shouldSideBeRendered(world, x, y, z, side);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileEnergyStorageCore core) {
            if (core.isOnline() && core.getTier() == 0) {
                core.validateStructure(false);
            }
        }
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block blockBroken, int metadata) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileEnergyStorageCore core) {
            if (core.isOnline() && core.getTier() == 0) {
                core.deactivateStabilizers();
            }
        }
        super.breakBlock(world, x, y, z, blockBroken, metadata);
    }

    @Override
    public AxisAlignedBB getSelectedBoundingBoxFromPool(World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileEnergyStorageCore core) {
            if (core.isOnline()) {
                return AxisAlignedBB.getBoundingBox(
                        core.xCoord + 0.5,
                        core.yCoord + 0.5,
                        core.zCoord + 0.5,
                        core.xCoord + 0.5,
                        core.yCoord + 0.5,
                        core.zCoord + 0.5);
            }
        }
        return super.getSelectedBoundingBoxFromPool(world, x, y, z);
    }

    @Override
    public List<String> getDisplayData(World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof TileEnergyStorageCore core)) {
            LogHelper.error("Missing Tile Entity (EnergyStorageCore getDisplayData)");
            return Collections.emptyList();
        }
        return core.getDisplayInformation(true);
    }
}
