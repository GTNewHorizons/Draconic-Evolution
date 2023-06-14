package com.brandon3055.draconicevolution.common.tileentities;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;

import com.brandon3055.draconicevolution.client.handler.ParticleHandler;
import com.brandon3055.draconicevolution.client.render.particle.ParticleCustom;
import com.brandon3055.draconicevolution.client.render.particle.Particles;
import com.brandon3055.draconicevolution.common.blocks.multiblock.MultiblockHelper.TileLocation;
import com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.TileEnergyStorageCore;
import com.brandon3055.draconicevolution.integration.computers.IDEPeripheral;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class TileParticleGenerator extends TileEntity implements IDEPeripheral {

    public static final int MAXIMUM_PARTICLE_INDEX = 3;
    public boolean isParticlesEnabled = true;

    public int red = 0;
    public int green = 0;
    public int blue = 0;
    public int randomRed = 0;
    public int randomGreen = 0;
    public int randomBlue = 0;
    public float motionX = 0.0F;
    public float motionY = 0.0F;
    public float motionZ = 0.0F;
    public float randomMotionX = 0.0F;
    public float randomMotionY = 0.0F;
    public float randomMotionZ = 0.0F;
    public float scale = 1F;
    public float randomScale = 0F;
    public int life = 100;
    public int randomLife = 0;
    public float spawnX = 0;
    public float spawnY = 0;
    public float spawnZ = 0;
    public float randomSpawnX = 0;
    public float randomSpawnY = 0;
    public float randomSpawnZ = 0;
    public int page = 1;
    public int fade = 0;
    public int spawnRate = 1;
    public boolean canParticleCollide = false;
    public int selectedParticle = 1;
    public float gravity = 0F;
    public boolean isActive = true;
    public boolean hasRedstoneSignal = false;
    public boolean isInverted = false;
    TileLocation master = new TileLocation();
    public float rotation = 0;
    public boolean isInStabilizerMode = false;

    // beam
    public boolean isBeamEnabled = false;
    public boolean shouldRenderCore = false;

    public int beamRed = 0;
    public int beamGreen = 0;
    public int beamBlue = 0;
    public float beamScale = 1F;
    public float beamPitch = 0F;
    public float beamYaw = 0F;
    public float beamLength = 0F;
    public float beamRotation = 0F;

    private int tick = 0;

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
        rotation += 0.5F;
        if (isInStabilizerMode) {
            spawnStabilizerParticle();
            return;
        }

        isActive = hasRedstoneSignal != isInverted;

        if (tick < spawnRate || !isActive || !isParticlesEnabled) {
            tick++;
            return;
        }

        tick = 0;
        Random rand = worldObj.rand;

        float motionX = this.motionX + (randomMotionX * rand.nextFloat());
        float motionY = this.motionY + (randomMotionY * rand.nextFloat());
        float motionZ = this.motionZ + (randomMotionZ * rand.nextFloat());
        float scale = this.scale + (randomScale * rand.nextFloat());
        double spawnX = xCoord + this.spawnX + (randomSpawnX * rand.nextFloat());
        double spawnY = yCoord + this.spawnY + (randomSpawnY * rand.nextFloat());
        double spawnZ = zCoord + this.spawnZ + (randomSpawnZ * rand.nextFloat());

        ParticleCustom particle = new ParticleCustom(
                worldObj,
                spawnX + 0.5,
                spawnY + 0.5,
                spawnZ + 0.5,
                motionX,
                motionY,
                motionZ,
                scale,
                canParticleCollide,
                this.selectedParticle);
        particle.red = this.red + rand.nextInt(randomRed + 1);
        particle.green = this.green + rand.nextInt(randomGreen + 1);
        particle.blue = this.blue + rand.nextInt(randomBlue + 1);
        particle.maxAge = this.life + rand.nextInt(randomLife + 1);
        particle.fadeTime = this.fade;
        particle.fadeLength = this.fade;
        particle.gravity = this.gravity;

        ParticleHandler.spawnCustomParticle(particle, 256);

    }

    @SideOnly(Side.CLIENT)
    private void spawnStabilizerParticle() {
        TileEnergyStorageCore core = getMaster();
        if (core == null || worldObj.getTotalWorldTime() % 20 != 1) {
            return;
        }

        double x = xCoord + 0.5;
        double y = yCoord + 0.5;
        double z = zCoord + 0.5;
        int direction;

        if (core.xCoord != xCoord) {
            direction = core.xCoord > xCoord ? 0 : 1;
        } else {
            direction = core.zCoord > zCoord ? 2 : 3;
        }

        Particles.EnergyBeamParticle particle = new Particles.EnergyBeamParticle(
                worldObj,
                x,
                y,
                z,
                core.xCoord + 0.5,
                core.zCoord + 0.5,
                direction,
                false);
        Particles.EnergyBeamParticle particle2 = new Particles.EnergyBeamParticle(
                worldObj,
                x,
                y,
                z,
                core.xCoord + 0.5,
                core.zCoord + 0.5,
                direction,
                true);
        ParticleHandler.spawnCustomParticle(particle, 60);
        ParticleHandler.spawnCustomParticle(particle2, 60);
    }

    public void toggleInverted() {
        isInverted = !isInverted;
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound tagCompound = new NBTTagCompound();
        this.writeToNBT(tagCompound);
        return new S35PacketUpdateTileEntity(this.xCoord, this.yCoord, this.zCoord, 1, tagCompound);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        readFromNBT(pkt.func_148857_g());
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        master.writeToNBT(compound, "Key");
        compound.setBoolean("StabalizerMode", isInStabilizerMode);
        getBlockNBT(compound);

        super.writeToNBT(compound);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        master.readFromNBT(compound, "Key");
        isInStabilizerMode = compound.getBoolean("StabalizerMode");
        setBlockNBT(compound);
        super.readFromNBT(compound);
    }

    public TileEnergyStorageCore getMaster() {
        TileEntity tile = master.getTileEntity(worldObj);
        return tile instanceof TileEnergyStorageCore core ? core : null;
    }

    public void setMaster(TileLocation master) {
        this.master = master != null ? master : new TileLocation();
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }

    public void getBlockNBT(NBTTagCompound compound) {
        compound.setInteger("Red", red);
        compound.setInteger("Green", green);
        compound.setInteger("Blue", blue);
        compound.setInteger("RandomRed", randomRed);
        compound.setInteger("RandomGreen", randomGreen);
        compound.setInteger("RandomBlue", randomBlue);
        compound.setFloat("MotionX", motionX);
        compound.setFloat("MotionY", motionY);
        compound.setFloat("MotionZ", motionZ);
        compound.setFloat("RandomMotionX", randomMotionX);
        compound.setFloat("RandomMotionY", randomMotionY);
        compound.setFloat("RandomMotionZ", randomMotionZ);
        compound.setFloat("Scale", scale);
        compound.setFloat("RandomScale", randomScale);
        compound.setInteger("Life", life);
        compound.setInteger("RandomLife", randomLife);
        compound.setFloat("SpawnX", spawnX);
        compound.setFloat("SpawnY", spawnY);
        compound.setFloat("SpawnZ", spawnZ);
        compound.setFloat("RandomSpawnX", randomSpawnX);
        compound.setFloat("RandomSpawnY", randomSpawnY);
        compound.setFloat("RandomSpawnZ", randomSpawnZ);
        compound.setInteger("Page", page);
        compound.setInteger("SpawnRate", spawnRate);
        compound.setBoolean("CanCollide", canParticleCollide);
        compound.setInteger("Fade", fade);
        compound.setInteger("SelectedParticle", selectedParticle);
        compound.setFloat("Gravity", gravity);
        compound.setBoolean("Active", isActive);
        compound.setBoolean("Signal", hasRedstoneSignal);
        compound.setBoolean("Inverted", isInverted);
        compound.setBoolean("particles_enabled", isParticlesEnabled);

        compound.setBoolean("beam_enabled", isBeamEnabled);
        compound.setBoolean("render_core", shouldRenderCore);
        compound.setInteger("beam_red", beamRed);
        compound.setInteger("beam_green", beamGreen);
        compound.setInteger("beam_blue", beamBlue);
        compound.setFloat("beam_scale", beamScale);
        compound.setFloat("beam_pitch", beamPitch);
        compound.setFloat("beam_yaw", beamYaw);
        compound.setFloat("beam_length", beamLength);
        compound.setFloat("beam_rotation", beamRotation);
    }

    public void setBlockNBT(NBTTagCompound compound) {
        red = compound.getInteger("Red");
        green = compound.getInteger("Green");
        blue = compound.getInteger("Blue");
        randomRed = compound.getInteger("RandomRed");
        randomGreen = compound.getInteger("RandomGreen");
        randomBlue = compound.getInteger("RandomBlue");
        motionX = compound.getFloat("MotionX");
        motionY = compound.getFloat("MotionY");
        motionZ = compound.getFloat("MotionZ");
        randomMotionX = compound.getFloat("RandomMotionX");
        randomMotionY = compound.getFloat("RandomMotionY");
        randomMotionZ = compound.getFloat("RandomMotionZ");
        scale = compound.getFloat("Scale");
        randomScale = compound.getFloat("RandomScale");
        life = compound.getInteger("Life");
        randomLife = compound.getInteger("RandomLife");
        spawnX = compound.getFloat("SpawnX");
        spawnY = compound.getFloat("SpawnY");
        spawnZ = compound.getFloat("SpawnZ");
        randomSpawnX = compound.getFloat("RandomSpawnX");
        randomSpawnY = compound.getFloat("RandomSpawnY");
        randomSpawnZ = compound.getFloat("RandomSpawnZ");
        page = compound.getInteger("Page");
        spawnRate = compound.getInteger("SpawnRate");
        canParticleCollide = compound.getBoolean("CanCollide");
        fade = compound.getInteger("Fade");
        selectedParticle = compound.getInteger("SelectedParticle");
        gravity = compound.getFloat("Gravity");
        isActive = compound.getBoolean("Active");
        hasRedstoneSignal = compound.getBoolean("Signal");
        isInverted = compound.getBoolean("Inverted");
        isParticlesEnabled = compound.getBoolean("particles_enabled");

        isBeamEnabled = compound.getBoolean("beam_enabled");
        shouldRenderCore = compound.getBoolean("render_core");
        beamRed = compound.getInteger("beam_red");
        beamGreen = compound.getInteger("beam_green");
        beamBlue = compound.getInteger("beam_blue");
        beamScale = compound.getFloat("beam_scale");
        beamPitch = compound.getFloat("beam_pitch");
        beamYaw = compound.getFloat("beam_yaw");
        beamLength = compound.getFloat("beam_length");
        beamRotation = compound.getFloat("beam_rotation");
    }

    @SideOnly(Side.CLIENT)
    public double getMaxRenderDistanceSquared() {
        return 655360.0D;
    }

    public static double limit(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }

    public static int limit(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    @Override
    public String getName() {
        return "particle_generator";
    }

    @Override
    public String[] getMethodNames() {
        return new String[] { "setGeneratorProperty", "getGeneratorState", "resetGeneratorState" };
    }

    @Override
    public Object[] callMethod(String method, Object... args) {
        if (method.startsWith("setGeneratorProperty")) {
            if (args.length != 2) {
                return new Object[] { false };
            } else if (!(args[0] instanceof String)) {
                return new Object[] { false };
            }

            /* Particles */
            if (args[0].equals("particles_enabled") && args[1] instanceof Boolean) {
                isParticlesEnabled = (boolean) args[1];
            } else if (args[0].equals("red") && args[1] instanceof Double) {
                red = limit(((Double) args[1]).intValue(), 0, 255);
            } else if (args[0].equals("green") && args[1] instanceof Double) {
                green = limit(((Double) args[1]).intValue(), 0, 255);
            } else if (args[0].equals("blue") && args[1] instanceof Double) {
                blue = limit(((Double) args[1]).intValue(), 0, 255);
            } else if (args[0].equals("random_red") && args[1] instanceof Double) {
                randomRed = limit(((Double) args[1]).intValue(), 0, 255);
            } else if (args[0].equals("random_green") && args[1] instanceof Double) {
                randomGreen = limit(((Double) args[1]).intValue(), 0, 255);
            } else if (args[0].equals("random_blue") && args[1] instanceof Double) {
                randomBlue = limit(((Double) args[1]).intValue(), 0, 255);
            } else if (args[0].equals("motion_x") && args[1] instanceof Double) {
                motionX = (float) limit((Double) args[1], -5F, 5F);
            } else if (args[0].equals("motion_y") && args[1] instanceof Double) {
                motionY = (float) limit((Double) args[1], -5F, 5F);
            } else if (args[0].equals("motion_z") && args[1] instanceof Double) {
                motionZ = (float) limit((Double) args[1], -5F, 5F);
            } else if (args[0].equals("random_motion_x") && args[1] instanceof Double) {
                randomMotionX = (float) limit((Double) args[1], -5F, 5F);
            } else if (args[0].equals("random_motion_y") && args[1] instanceof Double) {
                randomMotionY = (float) limit((Double) args[1], -5F, 5F);
            } else if (args[0].equals("random_motion_z") && args[1] instanceof Double) {
                randomMotionZ = (float) limit((Double) args[1], -5F, 5F);
            } else if (args[0].equals("scale") && args[1] instanceof Double) {
                scale = (float) limit((Double) args[1], 0.01F, 50F);
            } else if (args[0].equals("random_scale") && args[1] instanceof Double) {
                randomScale = (float) limit((Double) args[1], 0.01F, 50F);
            } else if (args[0].equals("life") && args[1] instanceof Double) {
                life = limit(((Double) args[1]).intValue(), 0, 1000);
            } else if (args[0].equals("random_life") && args[1] instanceof Double) {
                randomLife = limit(((Double) args[1]).intValue(), 0, 1000);
            } else if (args[0].equals("spawn_x") && args[1] instanceof Double) {
                spawnX = (float) limit((Double) args[1], -50F, 50F);
            } else if (args[0].equals("spawn_y") && args[1] instanceof Double) {
                spawnY = (float) limit((Double) args[1], -50F, 50F);
            } else if (args[0].equals("spawn_z") && args[1] instanceof Double) {
                spawnZ = (float) limit((Double) args[1], -50F, 50F);
            } else if (args[0].equals("random_spawn_x") && args[1] instanceof Double) {
                randomSpawnX = (float) limit((Double) args[1], -50F, 50F);
            } else if (args[0].equals("random_spawn_y") && args[1] instanceof Double) {
                randomSpawnY = (float) limit((Double) args[1], -50F, 50F);
            } else if (args[0].equals("random_spawn_z") && args[1] instanceof Double) {
                randomSpawnZ = (float) limit((Double) args[1], -50F, 50F);
            } else if (args[0].equals("fade") && args[1] instanceof Double) {
                fade = limit(((Double) args[1]).intValue(), 0, 100);
            } else if (args[0].equals("spawn_rate") && args[1] instanceof Double) {
                spawnRate = limit(((Double) args[1]).intValue(), 1, 200);
            } else if (args[0].equals("collide") && args[1] instanceof Boolean) {
                canParticleCollide = (Boolean) args[1];
            } else if (args[0].equals("selected_particle") && args[1] instanceof Double) {
                selectedParticle = limit(((Double) args[1]).intValue(), 1, MAXIMUM_PARTICLE_INDEX);
            } else if (args[0].equals("gravity") && args[1] instanceof Double) {
                gravity = (float) limit((Double) args[1], -5F, 5F);
            }
            /* Beam */
            else if (args[0].equals("beam_enabled") && args[1] instanceof Boolean) {
                isBeamEnabled = (Boolean) args[1];
            } else if (args[0].equals("render_core") && args[1] instanceof Boolean) {
                shouldRenderCore = (Boolean) args[1];
            } else if (args[0].equals("beam_red") && args[1] instanceof Double) {
                beamRed = limit(((Double) args[1]).intValue(), 0, 255);
            } else if (args[0].equals("beam_green") && args[1] instanceof Double) {
                beamGreen = limit(((Double) args[1]).intValue(), 0, 255);
            } else if (args[0].equals("beam_blue") && args[1] instanceof Double) {
                beamBlue = limit(((Double) args[1]).intValue(), 0, 255);
            } else if (args[0].equals("beam_scale") && args[1] instanceof Double) {
                beamScale = (float) limit((Double) args[1], -0F, 5F);
            } else if (args[0].equals("beam_pitch") && args[1] instanceof Double) {
                beamPitch = (float) limit((Double) args[1], -180F, 180F);
            } else if (args[0].equals("beam_yaw") && args[1] instanceof Double) {
                beamYaw = (float) limit((Double) args[1], -180F, 180F);
            } else if (args[0].equals("beam_length") && args[1] instanceof Double) {
                beamLength = (float) limit((Double) args[1], -0F, 320F);
            } else if (args[0].equals("beam_rotation") && args[1] instanceof Double) {
                beamRotation = (float) limit((Double) args[1], -1F, 1F);
            } else {
                return new Object[] { false };
            }

            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            return new Object[] { true };
        } else if (method.startsWith("getGeneratorState")) {
            Map<Object, Object> map = new HashMap<>();

            /* Particles */
            map.put("particles_enabled", isParticlesEnabled);
            map.put("red", red);
            map.put("green", green);
            map.put("blue", blue);
            map.put("random_red", randomRed);
            map.put("random_green", randomGreen);
            map.put("random_blue", randomBlue);
            map.put("motion_x", motionX);
            map.put("motion_y", motionY);
            map.put("motion_z", motionZ);
            map.put("random_motion_x", randomMotionX);
            map.put("random_motion_y", randomMotionY);
            map.put("random_motion_z", randomMotionZ);
            map.put("scale", scale);
            map.put("random_scale", randomScale);
            map.put("life", life);
            map.put("random_life", randomLife);
            map.put("spawn_x", spawnX);
            map.put("spawn_y", spawnY);
            map.put("spawn_z", spawnZ);
            map.put("random_spawn_x", randomSpawnX);
            map.put("random_spawn_y", randomSpawnY);
            map.put("random_spawn_z", randomSpawnZ);
            map.put("fade", fade);
            map.put("spawn_rate", spawnRate);
            map.put("collide", canParticleCollide);
            map.put("selected_particle", selectedParticle);
            map.put("gravity", gravity);

            /* Beam */
            map.put("beam_enabled", isBeamEnabled);
            map.put("render_core", shouldRenderCore);
            map.put("beam_red", beamRed);
            map.put("beam_green", beamGreen);
            map.put("beam_blue", beamBlue);
            map.put("beam_scale", beamScale);
            map.put("beam_pitch", beamPitch);
            map.put("beam_yaw", beamYaw);
            map.put("beam_length", beamLength);
            map.put("beam_rotation", beamRotation);

            return new Object[] { map };
        } else if (method.startsWith("resetGeneratorState")) {
            isParticlesEnabled = true;
            red = 0;
            green = 0;
            blue = 0;
            randomRed = 0;
            randomGreen = 0;
            randomBlue = 0;
            motionX = 0.0F;
            motionY = 0.0F;
            motionZ = 0.0F;
            randomMotionX = 0.0F;
            randomMotionY = 0.0F;
            randomMotionZ = 0.0F;
            scale = 1F;
            randomScale = 0F;
            life = 100;
            randomLife = 0;
            spawnX = 0;
            spawnY = 0;
            spawnZ = 0;
            randomSpawnX = 0;
            randomSpawnY = 0;
            randomSpawnZ = 0;
            page = 1;
            fade = 0;
            spawnRate = 1;
            canParticleCollide = false;
            selectedParticle = 1;
            gravity = 0F;

            // beam
            isBeamEnabled = false;
            shouldRenderCore = false;

            beamRed = 0;
            beamGreen = 0;
            beamBlue = 0;
            beamScale = 1F;
            beamPitch = 0F;
            beamYaw = 0F;
            beamLength = 0F;
            beamRotation = 0F;

            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            return new Object[] { true };
        }

        return new Object[] { 0 };
    }
}
