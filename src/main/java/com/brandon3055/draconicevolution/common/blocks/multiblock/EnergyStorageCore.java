package com.brandon3055.draconicevolution.common.blocks.multiblock;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.brandon3055.brandonscore.common.utills.InfoHelper;
import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.common.ModBlocks;
import com.brandon3055.draconicevolution.common.blocks.BlockDE;
import com.brandon3055.draconicevolution.common.lib.References;
import com.brandon3055.draconicevolution.common.lib.Strings;
import com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.TileEnergyStorageCore;
import com.brandon3055.draconicevolution.common.utills.IHudDisplayBlock;
import com.brandon3055.draconicevolution.common.utills.LogHelper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Created by Brandon on 25/07/2014.
 */
public class EnergyStorageCore extends BlockDE implements IHudDisplayBlock {

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
    public TileEntity createTileEntity(World world, int metadata) {
        return new TileEnergyStorageCore();
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int p_149727_6_,
            float p_149727_7_, float p_149727_8_, float p_149727_9_) {
        if (!world.isRemote) {
            TileEntity tile = world.getTileEntity(x, y, z);
            if (!(tile instanceof TileEnergyStorageCore)) {
                LogHelper.error("Missing Tile Entity (EnergyStorageCore)");
                return false;
            }
            TileEnergyStorageCore core = (TileEnergyStorageCore) tile;

            List<String> information = new ArrayList<>();
            core.addDisplayInformation(information);
            for (String line : information) {
                player.addChatComponentMessage(new ChatComponentText(line));
            }
        }
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
        TileEnergyStorageCore tile = (world.getTileEntity(
                x - ForgeDirection.getOrientation(side).offsetX,
                y - ForgeDirection.getOrientation(side).offsetY,
                z - ForgeDirection.getOrientation(side).offsetZ) != null
                && world.getTileEntity(
                        x - ForgeDirection.getOrientation(side).offsetX,
                        y - ForgeDirection.getOrientation(side).offsetY,
                        z - ForgeDirection.getOrientation(side).offsetZ) instanceof TileEnergyStorageCore)
                                ? (TileEnergyStorageCore) world.getTileEntity(
                                        x - ForgeDirection.getOrientation(side).offsetX,
                                        y - ForgeDirection.getOrientation(side).offsetY,
                                        z - ForgeDirection.getOrientation(side).offsetZ)
                                : null;
        // LogHelper.error(world.getTileEntity(x - ForgeDirection.getOrientation(side).offsetX, y -
        // ForgeDirection.getOrientation(side).offsetY, z - ForgeDirection.getOrientation(side).offsetZ));
        if (tile == null) {
            LogHelper.error("Missing Tile Entity (EnergyStorageCore)(shouldSideBeRendered)");
            return true;
        }
        if (tile.isOnline()) return false;
        else return super.shouldSideBeRendered(world, x, y, z, side);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block p_149695_5_) {
        TileEnergyStorageCore thisTile = (world.getTileEntity(x, y, z) != null
                && world.getTileEntity(x, y, z) instanceof TileEnergyStorageCore)
                        ? (TileEnergyStorageCore) world.getTileEntity(x, y, z)
                        : null;
        if (thisTile != null && thisTile.isOnline() && thisTile.getTier() == 0) {
            thisTile.isStructureStillValid(false);
        }
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block p_149749_5_, int p_149749_6_) {
        TileEnergyStorageCore thisTile = (world.getTileEntity(x, y, z) != null
                && world.getTileEntity(x, y, z) instanceof TileEnergyStorageCore)
                        ? (TileEnergyStorageCore) world.getTileEntity(x, y, z)
                        : null;
        if (thisTile != null && thisTile.isOnline() && thisTile.getTier() == 0) {
            thisTile.deactivateStabilizers();
        }
        super.breakBlock(world, x, y, z, p_149749_5_, p_149749_6_);
    }

    @Override
    public AxisAlignedBB getSelectedBoundingBoxFromPool(World world, int x, int y, int z) {
        TileEnergyStorageCore thisTile = (world.getTileEntity(x, y, z) != null
                && world.getTileEntity(x, y, z) instanceof TileEnergyStorageCore)
                        ? (TileEnergyStorageCore) world.getTileEntity(x, y, z)
                        : null;
        if (thisTile != null && thisTile.isOnline()) {
            return AxisAlignedBB.getBoundingBox(
                    thisTile.xCoord + 0.5,
                    thisTile.yCoord + 0.5,
                    thisTile.zCoord + 0.5,
                    thisTile.xCoord + 0.5,
                    thisTile.yCoord + 0.5,
                    thisTile.zCoord + 0.5);
        }
        return super.getSelectedBoundingBoxFromPool(world, x, y, z);
    }

    @Override
    public List<String> getDisplayData(World world, int x, int y, int z) {
        List<String> information = new ArrayList<>();

        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof TileEnergyStorageCore)) {
            LogHelper.error("Missing Tile Entity (EnergyStorageCore getDisplayData)");
            return information;
        }
        TileEnergyStorageCore core = (TileEnergyStorageCore) tile;
        information.add(InfoHelper.HITC() + getLocalizedName());
        core.addDisplayInformation(information);
        return information;
    }
}
