package com.brandon3055.draconicevolution.common.blocks.multiblock;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

import com.brandon3055.draconicevolution.common.blocks.multiblock.MultiblockHelper.TileLocation;
import com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.reactor.TileReactorCore;

/**
 * Created by Brandon on 23/7/2015.
 */
public interface IReactorPart {

    enum ComparatorMode {

        TEMPERATURE,
        TEMPERATURE_INVERTED,
        FIELD_CHARGE,
        FIELD_CHARGE_INVERTED,
        ENERGY_SATURATION,
        ENERGY_SATURATION_INVERTED,
        CONVERTED_FUEL,
        CONVERTED_FUEL_INVERTED;

        private static final ComparatorMode[] values = values();

        public static ComparatorMode getMode(int ordinal) {
            return ordinal >= 0 && ordinal < values.length ? values[ordinal] : TEMPERATURE;
        }

        public ComparatorMode next() {
            return values[(ordinal() + 1) % values.length];
        }

        public String toLocalizedString() {
            return StatCollector.translateToLocal("msg.de.reactorRSMode." + ordinal() + ".txt");
        }
    }

    static int getComparatorOutput(IBlockAccess world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof IReactorPart part) {
            TileReactorCore core = part.getMaster();
            if (core != null) {
                return core.getComparatorOutput(part.getComparatorMode());
            }
        }
        return 0;
    }

    TileLocation getMasterLocation();

    TileReactorCore getMaster();

    void setUp(TileLocation masterLocation);

    void shutDown();

    boolean isActive();

    ForgeDirection getFacing();

    ComparatorMode getComparatorMode();

    void changeComparatorMode();
}
