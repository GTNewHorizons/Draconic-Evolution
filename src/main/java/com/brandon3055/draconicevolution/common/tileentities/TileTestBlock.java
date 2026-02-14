package com.brandon3055.draconicevolution.common.tileentities;

import static net.minecraftforge.common.util.ForgeDirection.UP;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.util.ForgeDirection;

import com.brandon3055.draconicevolution.client.render.particle.ParticleEnergyBeam;
import com.brandon3055.draconicevolution.client.render.particle.ParticleEnergyField;
import com.brandon3055.draconicevolution.common.utils.EnergyStorage;
import com.brandon3055.draconicevolution.common.utils.LogHelper;
import com.brandon3055.draconicevolution.common.utils.Utils;

import cofh.api.energy.IEnergyHandler;
import cofh.api.energy.IEnergyReceiver;

/**
 * todo review Created by Brandon on 24/06/2014.
 */
public class TileTestBlock extends TileEntity implements IEnergyHandler {

    public EnergyStorage energy = new EnergyStorage(100000000);
    public int maxInput = 100000000;
    public float modelRotation;
    // Use a map for the beam to each target (Target, Beam)
    private ParticleEnergyBeam beam = null;
    private ParticleEnergyField ring = null;

    @Override
    public void updateEntity() {

        if (worldObj.isRemote) return;
        for (ForgeDirection d : ForgeDirection.VALID_DIRECTIONS) {
            if (worldObj.getTileEntity(
                    xCoord + d.offsetX,
                    yCoord + d.offsetY,
                    zCoord + d.offsetZ) instanceof IEnergyReceiver)
                ((IEnergyReceiver) worldObj.getTileEntity(xCoord + d.offsetX, yCoord + d.offsetY, zCoord + d.offsetZ))
                        .receiveEnergy(d.getOpposite(), Integer.MAX_VALUE, false);
        }
    }

    @Override
    public int receiveEnergy(ForgeDirection from, int maxReceive, boolean simulate) {
        if (from != UP) return 0; // this.energy.receiveEnergy(Math.min(maxInput, maxReceive), simulate);
        else return 0;
    }

    @Override
    public int extractEnergy(ForgeDirection from, int maxExtract, boolean simulate) {
        ;
        if (from == UP) return 0; // return this.energy.extractEnergy(maxExtract, simulate);
        else return 0;
    }

    @Override
    public int getEnergyStored(ForgeDirection from) {
        return energy.getEnergyStored();
    }

    @Override
    public int getMaxEnergyStored(ForgeDirection from) {
        return energy.getMaxEnergyStored();
    }

    @Override
    public boolean canConnectEnergy(ForgeDirection from) {
        return true;
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
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

    @Override
    public void readFromNBT(NBTTagCompound p_145839_1_) {
        super.readFromNBT(p_145839_1_);
        EntityPlayer player = null;
        if (worldObj != null) player = worldObj.getClosestPlayer(xCoord, yCoord, zCoord, -1);
        if (player != null) LogHelper
                .info("Read: " + Utils.getDistanceAtoB(player.posX, player.posY, player.posZ, xCoord, yCoord, zCoord));
        LogHelper.info(worldObj + " " + player);
    }

    @Override
    public void writeToNBT(NBTTagCompound p_145841_1_) {
        super.writeToNBT(p_145841_1_);
        EntityPlayer player = null;
        if (worldObj != null) player = worldObj.getClosestPlayer(xCoord, yCoord, zCoord, -1);
        if (player != null) LogHelper
                .info("Write: " + Utils.getDistanceAtoB(player.posX, player.posY, player.posZ, xCoord, yCoord, zCoord));
    }
}
