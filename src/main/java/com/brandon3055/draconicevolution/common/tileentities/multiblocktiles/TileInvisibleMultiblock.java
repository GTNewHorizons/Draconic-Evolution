package com.brandon3055.draconicevolution.common.tileentities.multiblocktiles;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

import com.brandon3055.draconicevolution.common.blocks.multiblock.InvisibleMultiblock;
import com.brandon3055.draconicevolution.common.blocks.multiblock.MultiblockHelper.TileLocation;
import com.brandon3055.draconicevolution.common.utills.LogHelper;

/**
 * Created by Brandon on 26/07/2014.
 */
public class TileInvisibleMultiblock extends TileEntity {

    public TileLocation master = new TileLocation();

    @Override
    public boolean canUpdate() {
        return false;
    }

    public boolean isMasterOnline() {
        TileEnergyStorageCore core = getMaster();
        return core != null && core.online;
    }

    public TileEnergyStorageCore getMaster() {
        if (master == null) return null;
        TileEntity tileEntity = worldObj.getTileEntity(master.getXCoord(), master.getYCoord(), master.getZCoord());
        if (tileEntity instanceof TileEnergyStorageCore) {
            return (TileEnergyStorageCore) tileEntity;
        }
        return null;
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        // if (master != null)
        master.writeToNBT(compound, "Key");
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        // if (master != null)
        master.readFromNBT(compound, "Key");
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound nbttagcompound = new NBTTagCompound();
        writeToNBT(nbttagcompound);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, nbttagcompound);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        readFromNBT(pkt.func_148857_g());
    }

    public void isStructureStillValid() {
        if (getMaster() == null) {
            LogHelper.error("{Tile} Master = null reverting!");
            InvisibleMultiblock.revert(worldObj, xCoord, yCoord, zCoord);
            return;
        }
        if (!getMaster().isOnline()) InvisibleMultiblock.revert(worldObj, xCoord, yCoord, zCoord);
    }
}
