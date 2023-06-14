package com.brandon3055.draconicevolution.client;

import net.minecraft.client.audio.ITickableSound;
import net.minecraft.client.audio.PositionedSound;
import net.minecraft.util.ResourceLocation;

import com.brandon3055.draconicevolution.common.lib.References;
import com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.reactor.TileReactorCore;

/**
 * Created by brandon3055 on 4/10/2015.
 */
public class ReactorSound extends PositionedSound implements ITickableSound {

    private static final ResourceLocation sound = new ResourceLocation(References.RESOURCESPREFIX + "coreSound");
    public boolean isDonePlaying = false;
    private final TileReactorCore core;

    public ReactorSound(TileReactorCore core) {
        super(sound);
        this.core = core;
        this.xPosF = (float) core.xCoord + 0.5F;
        this.yPosF = (float) core.yCoord + 0.5F;
        this.zPosF = (float) core.zCoord + 0.5F;
        this.repeat = true;
        this.volume = 1.5F;
    }

    @Override
    public boolean isDonePlaying() {
        return isDonePlaying;
    }

    @Override
    public void update() {
        volume = (core.renderSpeed - 0.5F) * 2F;
        if (core.reactionTemperature > 8000) {
            volume += (float) ((core.reactionTemperature - 8000D) / 1000D);
        }
        if (core.reactionTemperature > 2000 && core.maxFieldCharge > 0
                && core.fieldCharge < (core.maxFieldCharge * 0.2D)) {
            volume += 2D - ((core.fieldCharge / core.maxFieldCharge) * 10D);
        }
        if (core.reactionTemperature > 2000 && core.reactorFuel + core.convertedFuel > 0
                && core.reactorFuel < (double) (core.reactorFuel + core.convertedFuel) * 0.2D) {
            volume += 2D - ((float) core.reactorFuel / (core.reactorFuel + core.convertedFuel)) * 10D;
        }

        field_147663_c = 0.5F + volume / 2F;

        if (core.isInvalid() || !core.getWorldObj().getChunkFromBlockCoords(core.xCoord, core.zCoord).isChunkLoaded) {
            isDonePlaying = true;
            repeat = false;
        }
    }
}
