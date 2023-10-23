package com.brandon3055.draconicevolution.common.tileentities.multiblocktiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.util.ForgeDirection;

import com.brandon3055.draconicevolution.api.IExtendedRFStorage;
import com.brandon3055.draconicevolution.client.handler.ParticleHandler;
import com.brandon3055.draconicevolution.client.render.particle.Particles.EnergyTransferParticle;
import com.brandon3055.draconicevolution.common.ModBlocks;
import com.brandon3055.draconicevolution.common.blocks.multiblock.MultiblockHelper.TileLocation;
import com.brandon3055.draconicevolution.common.lib.References;
import com.brandon3055.draconicevolution.common.tileentities.TileObjectSync;
import com.brandon3055.draconicevolution.integration.computers.IDEPeripheral;

import cofh.api.energy.IEnergyHandler;
import cofh.api.energy.IEnergyReceiver;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Created by Brandon on 28/07/2014.
 */
public class TileEnergyPylon extends TileObjectSync implements IEnergyHandler, IExtendedRFStorage, IDEPeripheral {

    public boolean active = false;
    public boolean lastTickActive = false;
    public boolean isReceivingEnergy = false; // Power Flow to system
    public boolean isReceivedEnergyLastTick = false;
    public float modelRotation = 0;
    public float modelScale = 0;
    private List<TileLocation> coreLocations = new ArrayList<>();
    private int selectedCore = 0;
    private byte particleRate = 0;
    private byte lastTickParticleRate = 0;
    private int lastCheckCompOverride = 0;
    private int tick = 0;

    @Override
    public void updateEntity() {
        if (worldObj.isRemote) {
            if (active) {
                modelRotation += 1.5;
                modelScale += isReceivingEnergy ? 0.01F : -0.01F;
                if (modelScale < 0) {
                    modelScale = isReceivingEnergy ? 0F : 10000F;
                }
                spawnParticles();
            } else {
                modelScale = 0.5F;
            }
            return;
        }

        tick++;
        if (tick % 20 == 0) {
            int comparatorOut = (int) (getEnergyStored() / getMaxEnergyStored() * 15D);
            if (comparatorOut != lastCheckCompOverride) {
                worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord, zCoord, getBlockType());
                lastCheckCompOverride = comparatorOut;
            }
        }

        if (active && !isReceivingEnergy) {
            for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
                TileEntity tile = worldObj
                        .getTileEntity(xCoord + side.offsetX, yCoord + side.offsetY, zCoord + side.offsetZ);
                if (tile instanceof IEnergyReceiver receiver) {
                    int energyToReceive = extractEnergy(side, Integer.MAX_VALUE, true);
                    int energyReceived = receiver.receiveEnergy(side.getOpposite(), energyToReceive, false);
                    extractEnergy(side, energyReceived, false);
                }
            }
        }

        detectAndSendChanges();
        if (particleRate > 0) particleRate--;
    }

    public void onActivated() {
        if (!active) {
            active = isValidStructure();
        }
        findCores();
    }

    private TileEnergyStorageCore getMaster() {
        if (coreLocations.isEmpty()) return null;
        if (selectedCore >= coreLocations.size()) selectedCore = coreLocations.size() - 1;
        TileLocation location = coreLocations.get(selectedCore);
        if (location != null) {
            TileEntity tile = location.getTileEntity(worldObj);
            if (tile instanceof TileEnergyStorageCore core) {
                return core;
            }
        }
        return null;
    }

    private void findCores() {
        int yMod = worldObj.getBlockMetadata(xCoord, yCoord, zCoord) == 1 ? 15 : -15;
        int range = 15;
        List<TileLocation> locations = new ArrayList<>();
        for (int x = xCoord - range; x <= xCoord + range; x++) {
            for (int y = yCoord + yMod - range; y <= yCoord + yMod + range; y++) {
                for (int z = zCoord - range; z <= zCoord + range; z++) {
                    if (worldObj.getBlock(x, y, z) == ModBlocks.energyStorageCore) {
                        locations.add(new TileLocation(x, y, z));
                    }
                }
            }
        }

        if (locations != coreLocations) {
            coreLocations.clear();
            coreLocations.addAll(locations);
            selectedCore = selectedCore >= coreLocations.size() ? 0 : selectedCore;
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    public void nextCore() {
        findCores();
        selectedCore++;
        if (selectedCore >= coreLocations.size()) selectedCore = 0;
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    @SideOnly(Side.CLIENT)
    private void spawnParticles() {
        Random rand = worldObj.rand;

        TileEnergyStorageCore core = getMaster();
        if (core == null || !core.isOnline()) return;

        int x = core.xCoord;
        int y = core.yCoord;
        int z = core.zCoord;
        int cYCoord = worldObj.getBlockMetadata(xCoord, yCoord, zCoord) == 1 ? yCoord + 1 : yCoord - 1;

        float disMod = switch (core.getTier()) {
            case 0 -> 0.5F;
            case 1, 2 -> 1F;
            case 3, 4 -> 2F;
            case 5 -> 3F;
            default -> 4F;
        };

        if (particleRate > 20) particleRate = 20;
        double sourceX = x + 0.5 - disMod + (rand.nextFloat() * (disMod * 2));
        double sourceY = y + 0.5 - disMod + (rand.nextFloat() * (disMod * 2));
        double sourceZ = z + 0.5 - disMod + (rand.nextFloat() * (disMod * 2));
        double targetX = xCoord + 0.5;
        double targetY = cYCoord + 0.5;
        double targetZ = zCoord + 0.5;
        if (rand.nextFloat() < 0.05F) {
            EnergyTransferParticle passiveParticle = isReceivingEnergy
                    ? new EnergyTransferParticle(worldObj, targetX, targetY, targetZ, sourceX, sourceY, sourceZ, true)
                    : new EnergyTransferParticle(worldObj, sourceX, sourceY, sourceZ, targetX, targetY, targetZ, true);
            ParticleHandler.spawnCustomParticle(passiveParticle, 35);
        }
        if (particleRate > 0) {
            if (particleRate > 10 || rand.nextInt(Math.max(1, 10 - particleRate)) == 0) {
                int iterations = particleRate > 10 ? particleRate / 10 : 1;
                for (int i = 0; i <= iterations; i++) {
                    sourceX = x + 0.5 - disMod + (rand.nextFloat() * (disMod * 2));
                    sourceY = y + 0.5 - disMod + (rand.nextFloat() * (disMod * 2));
                    sourceZ = z + 0.5 - disMod + (rand.nextFloat() * (disMod * 2));
                    EnergyTransferParticle passiveParticle = isReceivingEnergy
                            ? new EnergyTransferParticle(
                                    worldObj,
                                    targetX,
                                    targetY,
                                    targetZ,
                                    sourceX,
                                    sourceY,
                                    sourceZ,
                                    false)
                            : new EnergyTransferParticle(
                                    worldObj,
                                    sourceX,
                                    sourceY,
                                    sourceZ,
                                    targetX,
                                    targetY,
                                    targetZ,
                                    false);
                    ParticleHandler.spawnCustomParticle(passiveParticle, 35);
                }
            }
        }
    }

    private boolean isValidStructure() {
        boolean hasGlassAbove = isGlass(xCoord, yCoord + 1, zCoord);
        boolean hasGlassBelow = isGlass(xCoord, yCoord - 1, zCoord);
        return hasGlassAbove != hasGlassBelow;
    }

    private boolean isGlass(int x, int y, int z) {
        return worldObj.getBlock(x, y, z) == ModBlocks.invisibleMultiblock && worldObj.getBlockMetadata(x, y, z) == 2;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        active = compound.getBoolean("Active");
        isReceivingEnergy = compound.getBoolean("Input");
        int i = compound.getInteger("Cores");
        List<TileLocation> list = new ArrayList<>();
        for (int j = 0; j < i; j++) {
            TileLocation l = new TileLocation();
            l.readFromNBT(compound, "Core" + j);
            list.add(l);
        }
        coreLocations = list;
        selectedCore = compound.getInteger("SelectedCore");
        particleRate = compound.getByte("ParticleRate");
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {

        super.writeToNBT(compound);
        compound.setBoolean("Active", active);
        compound.setBoolean("Input", isReceivingEnergy);
        int i = coreLocations.size();
        compound.setInteger("Cores", i);
        for (int j = 0; j < i; j++) {
            coreLocations.get(j).writeToNBT(compound, "Core" + j);
        }
        compound.setInteger("SelectedCore", selectedCore);
        compound.setByte("ParticleRate", particleRate);
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

    /* IEnergyHandler */
    @Override
    public boolean canConnectEnergy(ForgeDirection from) {
        return true;
    }

    @Override
    public int receiveEnergy(ForgeDirection from, int maxReceive, boolean simulate) {
        if (getMaster() == null) return 0;
        int received = isReceivingEnergy ? getMaster().receiveEnergy(maxReceive, simulate) : 0;
        if (!simulate && received > 0) particleRate = (byte) Math.min(20, received < 500 ? 1 : received / 500);
        return received;
    }

    @Override
    public int extractEnergy(ForgeDirection from, int maxExtract, boolean simulate) {
        if (getMaster() == null || !getMaster().isOnline()) return 0;
        int extracted = isReceivingEnergy ? 0 : getMaster().extractEnergy(maxExtract, simulate);
        if (!simulate && extracted > 0) particleRate = (byte) Math.min(20, extracted < 500 ? 1 : extracted / 500);
        return extracted;
    }

    @Override
    public int getEnergyStored(ForgeDirection from) {
        if (getMaster() == null) return 0;
        return (int) Math.min(Integer.MAX_VALUE, getMaster().getEnergyStored());
    }

    @Override
    public int getMaxEnergyStored(ForgeDirection from) {
        if (getMaster() == null) return 0;
        return (int) Math.min(Integer.MAX_VALUE, getMaster().getMaxEnergyStored());
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }

    private void detectAndSendChanges() {
        if (lastTickActive != active) lastTickActive = (Boolean) sendObjectToClient(
                References.BOOLEAN_ID,
                0,
                active,
                new NetworkRegistry.TargetPoint(worldObj.provider.dimensionId, xCoord, yCoord, zCoord, 256));
        if (isReceivedEnergyLastTick != isReceivingEnergy) isReceivedEnergyLastTick = (Boolean) sendObjectToClient(
                References.BOOLEAN_ID,
                1,
                isReceivingEnergy,
                new NetworkRegistry.TargetPoint(worldObj.provider.dimensionId, xCoord, yCoord, zCoord, 256));
        if (lastTickParticleRate != particleRate)
            lastTickParticleRate = (Byte) sendObjectToClient(References.BYTE_ID, 2, particleRate);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void receiveObjectFromServer(int index, Object object) {
        switch (index) {
            case 0 -> active = (Boolean) object;
            case 1 -> isReceivingEnergy = (Boolean) object;
            case 2 -> particleRate = (Byte) object;
        }
    }

    @Override
    public double getEnergyStored() {
        return getMaster() != null ? getMaster().getEnergyStored() : 0D;
    }

    @Override
    public double getMaxEnergyStored() {
        return getMaster() != null ? getMaster().getMaxEnergyStored() : 0D;
    }

    @Override
    public long getExtendedStorage() {
        return getMaster() != null ? getMaster().getEnergyStored() : 0L;
    }

    @Override
    public long getExtendedCapacity() {
        return getMaster() != null ? getMaster().getMaxEnergyStored() : 0L;
    }

    @Override
    public String getName() {
        return "draconic_rf_storage";
    }

    @Override
    public String[] getMethodNames() {
        return new String[] { "getEnergyStored", "getMaxEnergyStored" };
    }

    @Override
    public Object[] callMethod(String method, Object... args) {
        if (method.equals("getEnergyStored")) return new Object[] { getExtendedStorage() };
        else if (method.equals("getMaxEnergyStored")) return new Object[] { getExtendedCapacity() };
        return new Object[0];
    }
}
