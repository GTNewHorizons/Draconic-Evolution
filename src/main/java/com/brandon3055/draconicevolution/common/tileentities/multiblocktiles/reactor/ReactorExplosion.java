package com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.reactor;

import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

import com.brandon3055.draconicevolution.common.utils.Utils;
import com.brandon3055.draconicevolution.common.utils.handlers.IProcess;
import com.brandon3055.draconicevolution.common.utils.handlers.ProcessHandler;

/**
 * Created by brandon3055 on 12/8/2015.
 */
public class ReactorExplosion implements IProcess {

    public static final DamageSource FUSION_EXPLOSION = new DamageSource("damage.de.fusionExplode").setExplosion()
            .setDamageBypassesArmor().setDamageIsAbsolute().setDamageAllowedInCreativeMode();

    private final World world;
    private final int xCoord;
    private final int yCoord;
    private final int zCoord;
    private final float power;
    private boolean isDead;
    private double expansion = 0;

    public ReactorExplosion(World world, int x, int y, int z, float power) {
        this.world = world;
        this.xCoord = x;
        this.yCoord = y;
        this.zCoord = z;
        this.power = power;
        isDead = world.isRemote;
    }

    @Override
    public void updateProcess() {

        int size = (int) expansion;
        for (int x = xCoord - size; x < xCoord + size; x++) {
            for (int z = zCoord - size; z < zCoord + size; z++) {
                double distance = Utils.getDistanceAtoB(x, z, xCoord, zCoord);
                if (distance < expansion && distance >= size - 1) {
                    float tracePower = power - (float) (expansion / 10D);
                    tracePower *= 1F + (world.rand.nextFloat() - 0.5F) * 0.2;
                    ProcessHandler.addProcess(new ReactorExplosionTrace(world, x, yCoord, z, tracePower));
                }
            }
        }

        isDead = expansion >= power * 10;
        expansion += 1;
    }

    @Override
    public boolean isDead() {
        return isDead;
    }
}
