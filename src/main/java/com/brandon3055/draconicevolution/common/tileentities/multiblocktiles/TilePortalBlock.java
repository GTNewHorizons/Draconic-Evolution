package com.brandon3055.draconicevolution.common.tileentities.multiblocktiles;

import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.client.render.particle.Particles;
import com.brandon3055.draconicevolution.common.utils.Utils;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Created by Brandon on 24/5/2015.
 */
public class TilePortalBlock extends TileEntity {

    public int masterX = 0;
    public int masterY = 0;
    public int masterZ = 0;

    @Override
    @SideOnly(Side.SERVER)
    public boolean canUpdate() {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void updateEntity() {
        if (!worldObj.isRemote) {
            return;
        }
        double distanceMod = Utils.getDistanceAtoB(
                xCoord + 0.5,
                yCoord + 0.5,
                zCoord + 0.5,
                RenderManager.renderPosX,
                RenderManager.renderPosY,
                RenderManager.renderPosZ);
        if (worldObj.rand.nextInt(Math.max((int) (distanceMod * (distanceMod / 5D)), 1)) == 0) {
            getBlockMetadata();

            double offset1 = worldObj.rand.nextDouble();
            double offset2 = worldObj.rand.nextDouble();
            double smallOffset1 = -0.1 + worldObj.rand.nextDouble() * 0.2;
            double smallOffset2 = -0.1 + worldObj.rand.nextDouble() * 0.2;

            double sourceX = xCoord;
            double sourceY = yCoord;
            double sourceZ = zCoord;
            double targetX = xCoord;
            double targetY = yCoord;
            double targetZ = zCoord;

            switch (blockMetadata) {
                case 1 -> {
                    sourceX += offset1;
                    sourceY += offset2;
                    targetX += offset1 + smallOffset1;
                    targetY += offset2 + smallOffset2;
                    if (RenderManager.renderPosZ < zCoord + 0.5) {
                        targetZ += 0.75;
                    } else {
                        sourceZ += 1;
                        targetZ += 0.25;
                    }
                }
                case 2 -> {
                    sourceY += offset1;
                    sourceZ += offset2;
                    targetY += offset1 + smallOffset1;
                    targetZ += offset2 + smallOffset2;
                    if (RenderManager.renderPosX < xCoord + 0.5) {
                        targetX += 0.75;
                    } else {
                        sourceX += 1;
                        targetX += 0.25;
                    }
                }
                case 3 -> {
                    sourceX += offset1;
                    sourceZ += offset2;
                    targetX += offset1 + smallOffset1;
                    targetZ += offset2 + smallOffset2;
                    if (RenderManager.renderPosY < yCoord + 0.5) {
                        targetY += 0.75;
                    } else {
                        sourceY += 1;
                        targetY += 0.25;
                    }
                }
            }
            DraconicEvolution.proxy.spawnParticle(
                    new Particles.PortalParticle(worldObj, sourceX, sourceY, sourceZ, targetX, targetY, targetZ),
                    256);
        }
    }

    public TileDislocatorReceptacle getMaster() {
        TileEntity tile = worldObj.getTileEntity(masterX, masterY, masterZ);
        return tile instanceof TileDislocatorReceptacle receptacle ? receptacle : null;
    }

    public boolean isPortalStillValid() {
        TileDislocatorReceptacle receptacle = getMaster();
        if (receptacle == null || !receptacle.isActive) {
            return false;
        }
        receptacle.validateActivePortal();
        return receptacle.isActive;
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("MasterX", masterX);
        compound.setInteger("MasterY", masterY);
        compound.setInteger("MasterZ", masterZ);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        masterX = compound.getInteger("MasterX");
        masterY = compound.getInteger("MasterY");
        masterZ = compound.getInteger("MasterZ");
    }
}
