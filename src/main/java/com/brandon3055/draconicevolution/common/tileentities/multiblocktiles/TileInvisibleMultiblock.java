package com.brandon3055.draconicevolution.common.tileentities.multiblocktiles;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

import com.brandon3055.draconicevolution.common.blocks.multiblock.InvisibleMultiblock;
import com.brandon3055.draconicevolution.common.blocks.multiblock.MultiblockHelper.TileLocation;
import com.brandon3055.draconicevolution.common.utils.LogHelper;

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
        TileEntity tile = master.getTileEntity(worldObj);
        return tile instanceof TileEnergyStorageCore core && core.isOnline();
    }

    public TileEnergyStorageCore getMaster() {
        TileEntity tile = master.getTileEntity(worldObj);
        return tile instanceof TileEnergyStorageCore core ? core : null;
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        master.writeToNBT(compound, "Key");
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
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
        TileEnergyStorageCore core = getMaster();
        if (core == null) {
            LogHelper.error("{Tile} Master = null reverting!");
            InvisibleMultiblock.revertStructure(worldObj, xCoord, yCoord, zCoord);
            return;
        }
        if (!core.isOnline()) {
            InvisibleMultiblock.revertStructure(worldObj, xCoord, yCoord, zCoord);
        }
    }
}
