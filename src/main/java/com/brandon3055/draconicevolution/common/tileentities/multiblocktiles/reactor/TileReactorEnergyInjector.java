package com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.reactor;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.util.ForgeDirection;

import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.client.render.particle.ParticleReactorBeam;
import com.brandon3055.draconicevolution.common.blocks.multiblock.IReactorPart;
import com.brandon3055.draconicevolution.common.blocks.multiblock.MultiblockHelper.TileLocation;
import com.brandon3055.draconicevolution.integration.computers.IDEPeripheral;

import cofh.api.energy.IEnergyReceiver;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Created by Brandon on 23/7/2015.
 */
public class TileReactorEnergyInjector extends TileEntity implements IReactorPart, IEnergyReceiver, IDEPeripheral {

    public float modelIllumination = 1F;
    public ForgeDirection facing = ForgeDirection.UP;
    public TileLocation masterLocation = new TileLocation();
    public boolean isValid = false;

    @SideOnly(Side.CLIENT)
    private ParticleReactorBeam beam;

    private ComparatorMode comparatorMode = ComparatorMode.TEMPERATURE;
    private int comparatorOutputCache = -1;

    @Override
    public void updateEntity() {
        if (worldObj.isRemote && isValid) {
            updateBeam();
            return;
        }

        TileReactorCore core = getMaster();
        if (core != null) {
            int comparatorOutput = core.getComparatorOutput(comparatorMode);
            if (comparatorOutput != comparatorOutputCache) {
                comparatorOutputCache = comparatorOutput;
                worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord, zCoord, getBlockType());
            }
        }
    }

    @SideOnly(Side.CLIENT)
    private void updateBeam() {
        beam = DraconicEvolution.proxy.reactorBeam(this, beam, true);
    }

    public void onPlaced() {
        for (int distance = 1; distance <= TileReactorCore.MAXIMUM_PART_DISTANCE; distance++) {
            int targetX = xCoord + facing.offsetX * distance;
            int targetY = yCoord + facing.offsetY * distance;
            int targetZ = zCoord + facing.offsetZ * distance;
            TileEntity tile = worldObj.getTileEntity(targetX, targetY, targetZ);
            if (tile instanceof TileReactorCore) {
                setUp(new TileLocation(targetX, targetY, targetZ));
                return;
            }
        }
        shutDown();
    }

    @Override
    public TileLocation getMasterLocation() {
        return masterLocation;
    }

    @Override
    public TileReactorCore getMaster() {
        TileEntity tile = masterLocation.getTileEntity(worldObj);
        return tile instanceof TileReactorCore core ? core : null;
    }

    @Override
    public void setUp(TileLocation masterLocation) {
        this.masterLocation = masterLocation;
        isValid = true;
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    @Override
    public void shutDown() {
        masterLocation = new TileLocation();
        isValid = false;
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    @Override
    public boolean isActive() {
        return isValid;
    }

    @Override
    public ForgeDirection getFacing() {
        return facing;
    }

    @Override
    public ComparatorMode getComparatorMode() {
        return comparatorMode;
    }

    @Override
    public void changeComparatorMode() {
        comparatorMode = comparatorMode.next();
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound compound = new NBTTagCompound();
        masterLocation.writeToNBT(compound, "Master");
        compound.setInteger("Facing", facing.ordinal());
        compound.setBoolean("IsValid", isValid);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, compound);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        NBTTagCompound compound = pkt.func_148857_g();
        masterLocation.readFromNBT(compound, "Master");
        facing = ForgeDirection.getOrientation(compound.getInteger("Facing"));
        isValid = compound.getBoolean("IsValid");
        super.onDataPacket(net, pkt);
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        masterLocation.writeToNBT(compound, "Master");
        compound.setInteger("Facing", facing.ordinal());
        compound.setBoolean("IsValid", isValid);
        compound.setInteger("RedstoneMode", comparatorMode.ordinal());
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        masterLocation.readFromNBT(compound, "Master");
        facing = ForgeDirection.getOrientation(compound.getInteger("Facing"));
        isValid = compound.getBoolean("IsValid");
        comparatorMode = ComparatorMode.getMode(compound.getInteger("RedstoneMode"));
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return AxisAlignedBB.getBoundingBox(xCoord, yCoord, zCoord, xCoord + 1, yCoord + 1, zCoord + 1);
    }

    @Override
    public int receiveEnergy(ForgeDirection from, int maxReceive, boolean simulate) {
        if (simulate) {
            return Integer.MAX_VALUE;
        }
        TileReactorCore core = getMaster();
        if (core != null) {
            return core.injectEnergy(maxReceive);
        }
        return 0;
    }

    @Override
    public int getEnergyStored(ForgeDirection from) {
        return 0;
    }

    @Override
    public int getMaxEnergyStored(ForgeDirection from) {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean canConnectEnergy(ForgeDirection from) {
        return from == facing.getOpposite();
    }

    @Override
    public String getName() {
        return "draconic_reactor";
    }

    @Override
    public String[] getMethodNames() {
        return new String[] { "getReactorInfo", "chargeReactor", "activateReactor", "stopReactor" };
    }

    @Override
    public Object[] callMethod(String methodName, Object... args) {
        TileReactorCore core = getMaster();
        return core != null ? core.callMethod(methodName, args) : null;
    }
}
