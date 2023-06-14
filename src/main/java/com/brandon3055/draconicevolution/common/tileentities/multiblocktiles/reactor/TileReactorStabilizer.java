package com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.reactor;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.client.render.particle.ParticleReactorBeam;
import com.brandon3055.draconicevolution.common.blocks.multiblock.IReactorPart;
import com.brandon3055.draconicevolution.common.blocks.multiblock.MultiblockHelper.TileLocation;
import com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.reactor.TileReactorCore.ReactorState;
import com.brandon3055.draconicevolution.integration.computers.IDEPeripheral;

import cofh.api.energy.IEnergyProvider;
import cofh.api.energy.IEnergyReceiver;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Created by brandon3055 on 5/7/2015.
 */
public class TileReactorStabilizer extends TileEntity implements IReactorPart, IEnergyProvider, IDEPeripheral {

    public float coreRotation = 0F;
    public float ringRotation = 0F;
    public float coreSpeed = 1F;
    public float ringSpeed = 1F;
    public float modelIllumination = 0F;
    public ForgeDirection facing = ForgeDirection.UP;
    public TileLocation masterLocation = new TileLocation();
    public boolean isValid = false;
    public int tick = 0;
    private ComparatorMode comparatorMode = ComparatorMode.TEMPERATURE;
    private int comparatorOutputCache = -1;

    @SideOnly(Side.CLIENT)
    private ParticleReactorBeam beam;

    @Override
    public void updateEntity() {
        tick++;

        if (worldObj.isRemote) {
            updateBeam();
            return;
        }

        TileReactorCore core = getMaster();
        if (core != null) {
            if (core.reactorState == ReactorState.ONLINE) {
                ForgeDirection back = facing.getOpposite();
                TileEntity tile = worldObj
                        .getTileEntity(xCoord + back.offsetX, yCoord + back.offsetY, zCoord + back.offsetZ);
                if (tile instanceof IEnergyReceiver receiver) {
                    int energyToReceive = Math.min(core.energySaturation, core.maxEnergySaturation / 100);
                    int energyReceived = receiver.receiveEnergy(facing, energyToReceive, false);
                    core.energySaturation -= energyReceived;
                }
            }

            int comparatorOutput = core.getComparatorOutput(comparatorMode);
            if (comparatorOutput != comparatorOutputCache) {
                comparatorOutputCache = comparatorOutput;
                worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord, zCoord, blockType);
            }
        }
    }

    @SideOnly(Side.CLIENT)
    private void updateBeam() {
        TileReactorCore core = getMaster();
        if (core == null) {
            coreSpeed = 0;
            ringSpeed = 0;
            modelIllumination = 0;
            return;
        }
        coreRotation += coreSpeed;
        ringRotation += ringSpeed;
        coreSpeed = 30F * core.renderSpeed;
        ringSpeed = 5F * core.renderSpeed;
        modelIllumination = core.renderSpeed;
        if (tick % 100 == 0) {
            beam = null;
        }
        if (isValid) {
            beam = DraconicEvolution.proxy.reactorBeam(this, beam, true);
        }
    }

    public void onPlaced() {
        for (int distance = 1; distance <= TileReactorCore.MAXIMUM_PART_DISTANCE; distance++) {
            TileLocation location = new TileLocation(
                    xCoord + facing.offsetX * distance,
                    yCoord + facing.offsetY * distance,
                    zCoord + facing.offsetZ * distance);
            TileEntity tile = location.getTileEntity(worldObj);
            if (tile instanceof TileReactorCore core) {
                setUp(location);
                core.updateReactorParts(false);
                core.validateStructure();
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
        isValid = false;
        masterLocation = new TileLocation();
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
    public int extractEnergy(ForgeDirection from, int maxExtract, boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored(ForgeDirection from) {
        return 0;
    }

    @Override
    public int getMaxEnergyStored(ForgeDirection from) {
        return 0;
    }

    @Override
    public boolean canConnectEnergy(ForgeDirection from) {
        return from == facing.getOpposite();
    }

    @Override
    public double getMaxRenderDistanceSquared() {
        return 40960.0D;
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
