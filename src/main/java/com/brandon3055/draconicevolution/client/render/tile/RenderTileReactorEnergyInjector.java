package com.brandon3055.draconicevolution.client.render.tile;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

import com.brandon3055.draconicevolution.client.handler.ResourceHandler;
import com.brandon3055.draconicevolution.client.model.ModelReactorEnergyInjector;
import com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.reactor.TileReactorEnergyInjector;

/**
 * Created by Brandon on 6/7/2015.
 */
public class RenderTileReactorEnergyInjector extends TileEntitySpecialRenderer {

    public static ModelReactorEnergyInjector modelReactorEnergyInjector = new ModelReactorEnergyInjector();

    @Override
    public void renderTileEntityAt(TileEntity tileEntity, double x, double y, double z, float partialTick) {
        GL11.glPushMatrix();
        GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5);

        renderCore((TileReactorEnergyInjector) tileEntity);

        GL11.glPopMatrix();
    }

    public static void renderCore(TileReactorEnergyInjector injector) {
        GL11.glPushMatrix();
        float scale = (1F / 16F);

        int angle = 90;
        ForgeDirection axis = injector.facing;
        switch (injector.facing) {
            case UP:
                angle = 180;
                // it's intended to fall through
            case SOUTH:
                axis = ForgeDirection.WEST;
                break;
            case NORTH:
                axis = ForgeDirection.EAST;
                break;
            case WEST:
                axis = ForgeDirection.NORTH;
                break;
            case EAST:
                axis = ForgeDirection.SOUTH;
                break;
        }
        if (injector.facing != ForgeDirection.DOWN) {
            GL11.glRotated(angle, axis.offsetX, axis.offsetY, axis.offsetZ);
        }

        ResourceHandler.bindResource("textures/models/ModelReactorPowerInjector.png");
        modelReactorEnergyInjector.render(null, injector.modelIllumination, 0F, 0F, 0F, 0F, scale);

        GL11.glPopMatrix();
    }
}
