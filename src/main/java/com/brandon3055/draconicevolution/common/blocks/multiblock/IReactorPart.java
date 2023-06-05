package com.brandon3055.draconicevolution.common.blocks.multiblock;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

import com.brandon3055.draconicevolution.common.blocks.multiblock.MultiblockHelper.TileLocation;
import com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.reactor.TileReactorCore;

/**
 * Created by Brandon on 23/7/2015.
 */
public interface IReactorPart {

    int RMODE_TEMP = 0;
    int RMODE_TEMP_INV = 1;
    int RMODE_FIELD = 2;
    int RMODE_FIELD_INV = 3;
    int RMODE_SAT = 4;
    int RMODE_SAT_INV = 5;
    int RMODE_FUEL = 6;
    int RMODE_FUEL_INV = 7;

    static int getComparatorOutput(IBlockAccess world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof IReactorPart) {
            IReactorPart part = (IReactorPart) tile;
            TileReactorCore core = part.getMaster();
            if (core != null) {
                return core.getComparatorOutput(part.getRedstoneMode());
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

    int getRedstoneMode();

    void changeRedstoneMode();

    String getRedstoneModeAsString();
}
