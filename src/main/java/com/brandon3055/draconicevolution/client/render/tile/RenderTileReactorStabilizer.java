package com.brandon3055.draconicevolution.client.render.tile;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

import com.brandon3055.draconicevolution.client.handler.ResourceHandler;
import com.brandon3055.draconicevolution.client.model.ModelReactorStabilizerCore;
import com.brandon3055.draconicevolution.client.model.ModelReactorStabilizerRing;
import com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.reactor.TileReactorStabilizer;

/**
 * Created by Brandon on 6/7/2015.
 */
public class RenderTileReactorStabilizer extends TileEntitySpecialRenderer {

    public static ModelReactorStabilizerRing modelStabilizerRing = new ModelReactorStabilizerRing();
    public static ModelReactorStabilizerCore modelStabilizerCore = new ModelReactorStabilizerCore();

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTick) {
        if (!(tile instanceof TileReactorStabilizer stabilizer)) {
            return;
        }
        GL11.glPushMatrix();
        GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5);

        renderCore(stabilizer, partialTick);

        GL11.glPopMatrix();
    }

    public static void renderCore(TileReactorStabilizer stabilizer, float partialTick) {
        GL11.glPushMatrix();
        float scale = (1F / 16F);
        float coreRotation = stabilizer.coreRotation + (partialTick * stabilizer.coreSpeed);
        float ringRotation = stabilizer.ringRotation + (partialTick * stabilizer.ringSpeed);

        int angle = 90;
        ForgeDirection axis = stabilizer.facing;
        switch (stabilizer.facing) {
            case DOWN -> axis = ForgeDirection.WEST;
            case SOUTH -> {
                angle = 180;
                axis = ForgeDirection.EAST;
            }
            case UP -> axis = ForgeDirection.EAST;
            case WEST -> axis = ForgeDirection.UP;
            case EAST -> axis = ForgeDirection.DOWN;
        }
        if (stabilizer.facing != ForgeDirection.NORTH) {
            GL11.glRotated(angle, axis.offsetX, axis.offsetY, axis.offsetZ);
        }

        ResourceHandler.bindResource("textures/models/reactorStabilizerCore.png");
        modelStabilizerCore.render(null, coreRotation, stabilizer.modelIllumination, 0F, 0F, 0F, scale);

        ResourceHandler.bindResource("textures/models/reactorStabilizerRing.png");
        GL11.glRotated(90, 1, 0, 0);
        GL11.glTranslated(0, -0.58, 0);
        GL11.glScaled(0.95, 0.95, 0.95);
        GL11.glRotatef(ringRotation, 0, 1, 0);
        modelStabilizerRing.render(null, -70F, stabilizer.modelIllumination, 0F, 0F, 0F, scale);
        GL11.glPopMatrix();
    }
}
