package com.brandon3055.draconicevolution.common.tileentities.multiblocktiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.brandon3055.brandonscore.common.utills.LogHelper;
import com.brandon3055.draconicevolution.client.handler.ParticleHandler;
import com.brandon3055.draconicevolution.client.render.particle.Particles;
import com.brandon3055.draconicevolution.common.ModBlocks;
import com.brandon3055.draconicevolution.common.blocks.multiblock.MultiblockHelper.TileLocation;
import com.brandon3055.draconicevolution.common.lib.References;
import com.brandon3055.draconicevolution.common.tileentities.TileObjectSync;
import com.brandon3055.draconicevolution.integration.computers.IDEPeripheral;

import cofh.api.energy.IEnergyReceiver;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.interfaces.tileentity.IEnergyConnected;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.IFluidHandler;

/**
 * Created by Brandon on 28/07/2014.
 */
public class TileEnergyPylon extends TileObjectSync implements IDEPeripheral, IEnergyConnected {
    public boolean active = false;
    public boolean lastTickActive = false;
    public boolean reciveEnergy = false; //Power Flow to system
    public boolean lastTickReciveEnergy = false;
    public float modelRotation = 0;
    public float modelScale = 0;
    private List<TileLocation> coreLocatios = new ArrayList<TileLocation>();
    private int selectedCore = 0;
    private byte particleRate = 0;
    private byte lastTickParticleRate = 0;
    private int lastCheckCompOverride = 0;
    private int tick = 0;


    @Override
    public void updateEntity() {
        if (active && worldObj.isRemote) {
            modelRotation += 1.5;
            modelScale += !reciveEnergy ? -0.01F : 0.01F;
            if ((modelScale < 0 && !reciveEnergy)) modelScale = 10000F;
            if ((modelScale < 0 && reciveEnergy)) modelScale = 0F;
            spawnParticles();
        } else if (worldObj.isRemote) modelScale = 0.5F;

        if (worldObj.isRemote) return;

        tick++;
        if (tick % 20 == 0) {
            int cOut = (int) (getEnergyStored() / getMaxEnergyStored() * 15D);
            if (cOut != lastCheckCompOverride) {
                worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord, zCoord, worldObj.getBlock(xCoord, yCoord, zCoord));
                worldObj.notifyBlocksOfNeighborChange(xCoord - 1, yCoord, zCoord, worldObj.getBlock(xCoord, yCoord, zCoord));
                worldObj.notifyBlocksOfNeighborChange(xCoord + 1, yCoord, zCoord, worldObj.getBlock(xCoord, yCoord, zCoord));
                worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord - 1, zCoord, worldObj.getBlock(xCoord, yCoord, zCoord));
                worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord + 1, zCoord, worldObj.getBlock(xCoord, yCoord, zCoord));
                worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord, zCoord - 1, worldObj.getBlock(xCoord, yCoord, zCoord));
                worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord, zCoord + 1, worldObj.getBlock(xCoord, yCoord, zCoord));
                lastCheckCompOverride = cOut;
            }
        }
        /*
        if (active && !reciveEnergy) {
            for (ForgeDirection d : ForgeDirection.VALID_DIRECTIONS) {
                TileEntity tile = worldObj.getTileEntity(xCoord + d.offsetX, yCoord + d.offsetY, zCoord + d.offsetZ);
                if (tile != null && tile instanceof IEnergyReceiver) {
                    extractEnergy(d, ((IEnergyReceiver) tile).receiveEnergy(d.getOpposite(), extractEnergy(d, Integer.MAX_VALUE, true), false), false);
                }
            }
        }
    */
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
        if (coreLocatios.isEmpty()) return null;
        if (selectedCore >= coreLocatios.size()) selectedCore = coreLocatios.size() - 1;
        TileLocation core = coreLocatios.get(selectedCore);
        if (core == null || !(worldObj.getTileEntity(core.getXCoord(), core.getYCoord(), core.getZCoord()) instanceof TileEnergyStorageCore))
            return null;
        return (TileEnergyStorageCore) worldObj.getTileEntity(core.getXCoord(), core.getYCoord(), core.getZCoord());
    }

    private void findCores() {
        int yMod = worldObj.getBlockMetadata(xCoord, yCoord, zCoord) == 1 ? 15 : -15;
        int range = 15;
        List<TileLocation> locations = new ArrayList<TileLocation>();
        for (int x = xCoord - range; x <= xCoord + range; x++) {
            for (int y = yCoord + yMod - range; y <= yCoord + yMod + range; y++) {
                for (int z = zCoord - range; z <= zCoord + range; z++) {
                    if (worldObj.getBlock(x, y, z) == ModBlocks.energyStorageCore) {
                        locations.add(new TileLocation(x, y, z));
                    }
                }
            }
        }

        if (locations != coreLocatios) {
            coreLocatios.clear();
            coreLocatios.addAll(locations);
            selectedCore = selectedCore >= coreLocatios.size() ? 0 : selectedCore;
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    public void nextCore() {
        findCores();
        selectedCore++;
        if (selectedCore >= coreLocatios.size()) selectedCore = 0;
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    @SideOnly(Side.CLIENT)
    private void spawnParticles() {
        Random rand = worldObj.rand;
        if (getMaster() == null || !getMaster().isOnline()) return;

        int x = getMaster().xCoord;
        int y = getMaster().yCoord;
        int z = getMaster().zCoord;
        int cYCoord = worldObj.getBlockMetadata(xCoord, yCoord, zCoord) == 1 ? yCoord + 1 : yCoord - 1;

        float disMod = getMaster().getTier() == 0 ? 0.5F : getMaster().getTier() == 1 ? 1F : getMaster().getTier() == 2 ? 1F : getMaster().getTier() == 3 ? 2F : getMaster().getTier() == 4 ? 2F : getMaster().getTier() == 5 ? 3F : 4F;
        double spawnX;
        double spawnY;
        double spawnZ;
        double targetX;
        double targetY;
        double targetZ;
        if (particleRate > 20) particleRate = 20;
        if (!reciveEnergy) {
            spawnX = x + 0.5 - disMod + (rand.nextFloat() * (disMod * 2));
            spawnY = y + 0.5 - disMod + (rand.nextFloat() * (disMod * 2));
            spawnZ = z + 0.5 - disMod + (rand.nextFloat() * (disMod * 2));
            targetX = xCoord + 0.5;
            targetY = cYCoord + 0.5;
            targetZ = zCoord + 0.5;
            if (rand.nextFloat() < 0.05F) {
                Particles.EnergyTransferParticle passiveParticle = new Particles.EnergyTransferParticle(worldObj, spawnX, spawnY, spawnZ, targetX, targetY, targetZ, true);
                ParticleHandler.spawnCustomParticle(passiveParticle, 35);
            }
            if (particleRate > 0) {
                if (particleRate > 10) {
                    for (int i = 0; i <= particleRate / 10; i++) {
                        spawnX = x + 0.5 - disMod + (rand.nextFloat() * (disMod * 2));
                        spawnY = y + 0.5 - disMod + (rand.nextFloat() * (disMod * 2));
                        spawnZ = z + 0.5 - disMod + (rand.nextFloat() * (disMod * 2));
                        Particles.EnergyTransferParticle passiveParticle = new Particles.EnergyTransferParticle(worldObj, spawnX, spawnY, spawnZ, targetX, targetY, targetZ, false);
                        ParticleHandler.spawnCustomParticle(passiveParticle, 35);
                    }
                } else if (rand.nextInt(Math.max(1, 10 - particleRate)) == 0) {
                    spawnX = x + 0.5 - disMod + (rand.nextFloat() * (disMod * 2));
                    spawnY = y + 0.5 - disMod + (rand.nextFloat() * (disMod * 2));
                    spawnZ = z + 0.5 - disMod + (rand.nextFloat() * (disMod * 2));
                    Particles.EnergyTransferParticle passiveParticle = new Particles.EnergyTransferParticle(worldObj, spawnX, spawnY, spawnZ, targetX, targetY, targetZ, false);
                    ParticleHandler.spawnCustomParticle(passiveParticle, 35);
                }
            }

        } else {
            targetX = x + 0.5 - disMod + (rand.nextFloat() * (disMod * 2));
            targetY = y + 0.5 - disMod + (rand.nextFloat() * (disMod * 2));
            targetZ = z + 0.5 - disMod + (rand.nextFloat() * (disMod * 2));
            spawnX = xCoord + 0.5;
            spawnY = cYCoord + 0.5;
            spawnZ = zCoord + 0.5;
            if (rand.nextFloat() < 0.05F) {
                Particles.EnergyTransferParticle passiveParticle = new Particles.EnergyTransferParticle(worldObj, spawnX, spawnY, spawnZ, targetX, targetY, targetZ, true);
                ParticleHandler.spawnCustomParticle(passiveParticle, 35);
            }

            if (particleRate > 0) {
                if (particleRate > 10) {
                    for (int i = 0; i <= particleRate / 10; i++) {
                        targetX = x + 0.5 - disMod + (rand.nextFloat() * (disMod * 2));
                        targetY = y + 0.5 - disMod + (rand.nextFloat() * (disMod * 2));
                        targetZ = z + 0.5 - disMod + (rand.nextFloat() * (disMod * 2));
                        Particles.EnergyTransferParticle passiveParticle = new Particles.EnergyTransferParticle(worldObj, spawnX, spawnY, spawnZ, targetX, targetY, targetZ, false);
                        ParticleHandler.spawnCustomParticle(passiveParticle, 35);
                    }
                } else if (rand.nextInt(Math.max(1, 10 - particleRate)) == 0) {
                    targetX = x + 0.5 - disMod + (rand.nextFloat() * (disMod * 2));
                    targetY = y + 0.5 - disMod + (rand.nextFloat() * (disMod * 2));
                    targetZ = z + 0.5 - disMod + (rand.nextFloat() * (disMod * 2));
                    Particles.EnergyTransferParticle passiveParticle = new Particles.EnergyTransferParticle(worldObj, spawnX, spawnY, spawnZ, targetX, targetY, targetZ, false);
                    ParticleHandler.spawnCustomParticle(passiveParticle, 35);
                }
            }
        }
    }

    private boolean isValidStructure() {
        return (isGlass(xCoord, yCoord + 1, zCoord) || isGlass(xCoord, yCoord - 1, zCoord)) && (!isGlass(xCoord, yCoord + 1, zCoord) || !isGlass(xCoord, yCoord - 1, zCoord));
    }

    private boolean isGlass(int x, int y, int z) {
        return worldObj.getBlock(x, y, z) == ModBlocks.invisibleMultiblock && worldObj.getBlockMetadata(x, y, z) == 2;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        active = compound.getBoolean("Active");
        reciveEnergy = compound.getBoolean("Input");
        int i = compound.getInteger("Cores");
        List<TileLocation> list = new ArrayList<TileLocation>();
        for (int j = 0; j < i; j++) {
            TileLocation l = new TileLocation();
            l.readFromNBT(compound, "Core" + j);
            list.add(l);
        }
        coreLocatios = list;
        selectedCore = compound.getInteger("SelectedCore");
        particleRate = compound.getByte("ParticleRate");
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {

        super.writeToNBT(compound);
        compound.setBoolean("Active", active);
        compound.setBoolean("Input", reciveEnergy);
        int i = coreLocatios.size();
        compound.setInteger("Cores", i);
        for (int j = 0; j < i; j++) {
            coreLocatios.get(j).writeToNBT(compound, "Core" + j);
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

    @Override
    public long injectEnergyUnits(byte aSide, long aVoltage, long aAmperage) {
        
        LogHelper.warn("Blah blah");
        
        if (getMaster() == null) return 0;
        long totalEnergyAccumalated = 0L;
        
        while (inputEnergyFrom(aSide)) {
             totalEnergyAccumalated += (aVoltage * aAmperage); // Must do once per tick
        }
        
        getMaster().receiveEnergy(totalEnergyAccumalated, false);
        
        return aAmperage; // *Should* allow for infinite amps?? Maybe set it to something specific
    }
    
    //Don't care about sides
    @Override
    public boolean inputEnergyFrom(byte aSide) {
        return true;
    }
    
    @Override
    public boolean outputsEnergyTo(byte aSide) {
        return true;
    }
    
    
    /* Old RF fuckery
    @Override
    public int receiveEnergy(ForgeDirection from, int maxReceive, boolean simulate) {
        if (getMaster() == null) return 0;
        int received = reciveEnergy ? getMaster().receiveEnergy(maxReceive, simulate) : 0;
        if (!simulate && received > 0)
            particleRate = (byte) Math.min(20, received < 500 && received > 0 ? 1 : received / 500);
        return received;
    }
     
    
    @Override
    public int extractEnergy(ForgeDirection from, int maxExtract, boolean simulate) {
        if (getMaster() == null || !getMaster().isOnline()) return 0;
        int extracted = reciveEnergy ? 0 : getMaster().extractEnergy(maxExtract, simulate);
        if (!simulate && extracted > 0)
            particleRate = (byte) Math.min(20, extracted < 500 && extracted > 0 ? 1 : extracted / 500);
        return extracted;
    }
    */

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }

    private void detectAndSendChanges() {
        if (lastTickActive != active)
            lastTickActive = (Boolean) sendObjectToClient(References.BOOLEAN_ID, 0, active, new NetworkRegistry.TargetPoint(worldObj.provider.dimensionId, xCoord, yCoord, zCoord, 256));
        if (lastTickReciveEnergy != reciveEnergy)
            lastTickReciveEnergy = (Boolean) sendObjectToClient(References.BOOLEAN_ID, 1, reciveEnergy, new NetworkRegistry.TargetPoint(worldObj.provider.dimensionId, xCoord, yCoord, zCoord, 256));
        if (lastTickParticleRate != particleRate)
            lastTickParticleRate = (Byte) sendObjectToClient(References.BYTE_ID, 2, particleRate);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void receiveObjectFromServer(int index, Object object) {
        switch (index) {
            case 0:
                active = (Boolean) object;
                break;
            case 1:
                reciveEnergy = (Boolean) object;
                break;
            case 2:
                particleRate = (Byte) object;
                break;
        }
    }

    public double getEnergyStored() {
        return getMaster() != null ? getMaster().getEnergyStored() : 0D;
    }

    public double getMaxEnergyStored() {
        return getMaster() != null ? getMaster().getMaxEnergyStored() : 0D;
    }

    public long getExtendedStorage() {
        return getMaster() != null ? getMaster().getEnergyStored() : 0L;
    }

    public long getExtendedCapacity() {
        return getMaster() != null ? getMaster().getMaxEnergyStored() : 0L;
    }

    @Override
    public String getName() {
        return "draconic_rf_storage";
    }

    @Override
    public String[] getMethodNames() {
        return new String[]{"getEnergyStored", "getMaxEnergyStored"};
    }

    @Override
    public Object[] callMethod(String method, Object... args) {
        if (method.equals("getEnergyStored")) return new Object[]{getExtendedStorage()};
        else if (method.equals("getMaxEnergyStored")) return new Object[]{getExtendedCapacity()};
        return new Object[0];
    }

    // ALL of below is bs that is really only necessary to not displease daddy inheritance
    
    @Override
    public byte getColorization() {
        // TODO Auto-generated method stub
        return -1;
    }

    @Override
    public byte setColorization(byte arg0) {
        // TODO Auto-generated method stub
        return -1;
    }

    @Override
    public boolean getAir(int arg0, int arg1, int arg2) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean getAirAtSide(byte arg0) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean getAirAtSideAndDistance(byte arg0, int arg1) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean getAirOffset(int arg0, int arg1, int arg2) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public BiomeGenBase getBiome() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BiomeGenBase getBiome(int arg0, int arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Block getBlock(int arg0, int arg1, int arg2) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Block getBlockAtSide(byte arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Block getBlockAtSideAndDistance(byte arg0, int arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Block getBlockOffset(int arg0, int arg1, int arg2) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IGregTechTileEntity getIGregTechTileEntity(int arg0, int arg1, int arg2) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IGregTechTileEntity getIGregTechTileEntityAtSide(byte arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IGregTechTileEntity getIGregTechTileEntityAtSideAndDistance(byte arg0, int arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IGregTechTileEntity getIGregTechTileEntityOffset(int arg0, int arg1, int arg2) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IInventory getIInventory(int arg0, int arg1, int arg2) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IInventory getIInventoryAtSide(byte arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IInventory getIInventoryAtSideAndDistance(byte arg0, int arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IInventory getIInventoryOffset(int arg0, int arg1, int arg2) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IFluidHandler getITankContainer(int arg0, int arg1, int arg2) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IFluidHandler getITankContainerAtSide(byte arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IFluidHandler getITankContainerAtSideAndDistance(byte arg0, int arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IFluidHandler getITankContainerOffset(int arg0, int arg1, int arg2) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public byte getLightLevel(int arg0, int arg1, int arg2) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public byte getLightLevelAtSide(byte arg0) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public byte getLightLevelAtSideAndDistance(byte arg0, int arg1) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public byte getLightLevelOffset(int arg0, int arg1, int arg2) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public byte getMetaID(int arg0, int arg1, int arg2) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public byte getMetaIDAtSide(byte arg0) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public byte getMetaIDAtSideAndDistance(byte arg0, int arg1) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public byte getMetaIDOffset(int arg0, int arg1, int arg2) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getOffsetX(byte arg0, int arg1) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public short getOffsetY(byte arg0, int arg1) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getOffsetZ(byte arg0, int arg1) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean getOpacity(int arg0, int arg1, int arg2) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean getOpacityAtSide(byte arg0) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean getOpacityAtSideAndDistance(byte arg0, int arg1) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean getOpacityOffset(int arg0, int arg1, int arg2) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int getRandomNumber(int arg0) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean getSky(int arg0, int arg1, int arg2) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean getSkyAtSide(byte arg0) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean getSkyAtSideAndDistance(byte arg0, int arg1) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean getSkyOffset(int arg0, int arg1, int arg2) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public TileEntity getTileEntity(int arg0, int arg1, int arg2) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TileEntity getTileEntityAtSide(byte arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TileEntity getTileEntityAtSideAndDistance(byte arg0, int arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TileEntity getTileEntityOffset(int arg0, int arg1, int arg2) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getTimer() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public World getWorld() {
        // TODO Auto-generated method stub
        return getWorldObj();
    }

    @Override
    public int getXCoord() {
        // TODO Auto-generated method stub
        return xCoord;
    }

    @Override
    public short getYCoord() {
        // TODO Auto-generated method stub
        return (short) yCoord;
    }

    @Override
    public int getZCoord() {
        // TODO Auto-generated method stub
        return zCoord;
    }

    @Override
    public boolean isClientSide() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isDead() {
        // TODO Auto-generated method stub
        return tileEntityInvalid;
    }

    @Override
    public boolean isInvalidTileEntity() {
        // TODO Auto-generated method stub
        return tileEntityInvalid;
    }

    @Override
    public boolean isServerSide() {
        // TODO Auto-generated method stub
        return true; // I guess??
    }

    @Override
    public boolean openGUI(EntityPlayer arg0) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean openGUI(EntityPlayer arg0, int arg1) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void sendBlockEvent(byte arg0, byte arg1) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setLightValue(byte arg0) {
        // TODO Auto-generated method stub
        
    }
}
