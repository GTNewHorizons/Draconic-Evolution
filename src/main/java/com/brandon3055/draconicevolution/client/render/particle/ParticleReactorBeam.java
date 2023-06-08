package com.brandon3055.draconicevolution.client.render.particle;

import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.tileentity.TileEntity;

import org.lwjgl.opengl.GL11;

import com.brandon3055.draconicevolution.client.handler.ResourceHandler;
import com.brandon3055.draconicevolution.common.blocks.multiblock.IReactorPart;
import com.brandon3055.draconicevolution.common.blocks.multiblock.MultiblockHelper;
import com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.reactor.TileReactorCore;
import com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.reactor.TileReactorStabilizer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Created by Brandon on 17/7/2015.
 */
@SideOnly(Side.CLIENT)
public class ParticleReactorBeam extends EntityFX {

    private final TileEntity tile;

    public ParticleReactorBeam(TileEntity tile) {
        super(tile.getWorldObj(), tile.xCoord + 0.5, tile.yCoord + 0.5, tile.zCoord + 0.5, 0.0, 0.0, 0.0);
        this.tile = tile;
        this.particleRed = 1F;
        this.particleGreen = 1F;
        this.particleBlue = 1F;
        this.noClip = true;
        this.motionX = 0.0D;
        this.motionY = 0.0D;
        this.motionZ = 0.0D;
        this.particleMaxAge = 4;
        this.setSize(1F, 1F);
    }

    public void update() {
        if (this.particleMaxAge - this.particleAge < 4) {
            this.particleMaxAge = this.particleAge + 4;
        }
    }

    @Override
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;

        if (this.particleAge++ >= this.particleMaxAge) {
            this.setDead();
        }
    }

    @Override
    public void renderParticle(Tessellator tessellator, float partialTick, float rotX, float rotXZ, float rotZ,
            float rotYZ, float rotXY) {
        if (!((IReactorPart) tile).isActive()) {
            return;
        }
        tessellator.draw();
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.0F);

        double shiftX = this.prevPosX + (this.posX - this.prevPosX) * partialTick - interpPosX;
        double shiftY = this.prevPosY + (this.posY - this.prevPosY) * partialTick - interpPosY;
        double shiftZ = this.prevPosZ + (this.posZ - this.prevPosZ) * partialTick - interpPosZ;
        GL11.glTranslated(shiftX, shiftY, shiftZ);

        // Common Fields
        MultiblockHelper.TileLocation master = ((IReactorPart) tile).getMasterLocation();
        double offsetX = master.posX - tile.xCoord;
        double offsetY = master.posY - tile.yCoord;
        double offsetZ = master.posZ - tile.zCoord;
        double offsetLength = Math.sqrt(offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ);
        double diagonalLength = Math.sqrt(offsetX * offsetX + offsetZ * offsetZ);

        // Rotate beam to face target
        GL11.glRotated(Math.toDegrees(-Math.atan2(offsetZ, offsetX)) - 90.0, 0.0, 1.0, 0.0);
        GL11.glRotated(Math.toDegrees(-Math.atan2(diagonalLength, offsetY)) - 90.0, 1.0, 0.0, 0.0);

        TileReactorCore core = ((IReactorPart) tile).getMaster();

        if (tile instanceof TileReactorStabilizer && core != null)
            renderStabilizerEffect(tessellator, core, offsetLength, partialTick);
        else if (core != null) {
            GL11.glShadeModel(GL11.GL_SMOOTH);
            GL11.glTranslated(0.0, 0.0, 0.3);
            GL11.glRotatef(((float) particleAge + partialTick) * 20F, 0F, 0F, 1F);
            ResourceHandler.bindResource("textures/particle/reactorEnergyBeam.png");
            double sizeOrigin = 0.5;

            int color = 0xFF2200;
            int alpha = (int) (200.0 * core.renderSpeed);

            double speed = 20.0;
            double texV2 = ((double) particleAge + partialTick) * 0.01 * speed;
            double texV1 = offsetLength + texV2;

            tessellator.startDrawing(GL11.GL_TRIANGLE_STRIP);
            tessellator.setBrightness(200);

            double sizeTarget = 0.1;

            final int raysCount = 16;
            for (int ray = 0; ray <= raysCount; ++ray) {
                double texU = (double) ray / raysCount;
                double verX = Math.sin(texU * Math.PI * 2.3) * sizeTarget;
                double verY = Math.cos(texU * Math.PI * 2.3) * sizeTarget;

                tessellator.setColorRGBA((color & 0xFF0000) >> 16, (color & 0xFF00) >> 8, color & 0xFF, 0);
                tessellator.addVertexWithUV(verX * sizeOrigin, verY * sizeOrigin, -0.55, texU, texV1);
                tessellator.setColorRGBA((color & 0xFF0000) >> 16, (color & 0xFF00) >> 8, color & 0xFF, alpha);
                tessellator.addVertexWithUV(verX, verY, offsetLength, texU, texV2);
            }

            offsetLength -= core.getCoreRadius() + 0.3;
            sizeTarget = core.getCoreRadius() * 0.2;
            sizeOrigin = 0.1 / sizeTarget;

            for (int ray = 0; ray <= raysCount; ray++) {
                double texU = (double) ray / raysCount;
                double verX = Math.sin(texU * Math.PI * 2.13) * sizeTarget;
                double verY = Math.cos(texU * Math.PI * 2.13) * sizeTarget;

                tessellator.setColorRGBA((color & 0xFF0000) >> 16, (color & 0xFF00) >> 8, color & 0xFF, alpha);
                tessellator.addVertexWithUV(verX * sizeOrigin, verY * sizeOrigin, 0.0, texU, texV1);
                tessellator.setColorRGBA((color & 0xFF0000) >> 16, (color & 0xFF00) >> 8, (color & 0xFF), 0);
                tessellator.addVertexWithUV(verX, verY, offsetLength, texU, texV2);
            }

            tessellator.draw();
            GL11.glRotatef(((float) particleAge + partialTick) * -30F, 0F, 0F, 1F);

            tessellator.startDrawing(GL11.GL_TRIANGLE_STRIP);
            tessellator.setBrightness(200);
            color = 0xFF4400;

            sizeTarget = core.getCoreRadius() * 0.6;
            sizeOrigin = 0.1 / sizeTarget;
            offsetLength += 0.4;
            GL11.glTranslated(0, 0, -0.1);

            for (int ray = 0; ray <= raysCount; ray++) {
                double texU = (double) ray / raysCount;
                double verX = Math.sin(texU * Math.PI * 2.13F) * sizeTarget;
                double verY = Math.cos(texU * Math.PI * 2.13F) * sizeTarget;

                tessellator.setColorRGBA((color & 0xFF0000) >> 16, (color & 0xFF00) >> 8, color & 0xFF, alpha / 2);
                tessellator.addVertexWithUV(verX * sizeOrigin, verY * sizeOrigin, 0.0, texU, texV1);
                tessellator.setColorRGBA((color & 0xFF0000) >> 16, (color & 0xFF00) >> 8, color & 0xFF, 0);
                tessellator.addVertexWithUV(verX, verY, offsetLength, texU, texV2);
            }

            tessellator.draw();
            GL11.glShadeModel(GL11.GL_FLAT);
        }

        GL11.glPopMatrix();
        GL11.glEnable(GL11.GL_CULL_FACE);
        ResourceHandler.bindDefaultParticles();
        tessellator.startDrawingQuads();
    }

    private void renderStabilizerEffect(Tessellator tessellator, TileReactorCore core, double offsetLength,
            float partialTick) {
        GL11.glShadeModel(GL11.GL_SMOOTH);

        // Draw Beams
        GL11.glPushMatrix();
        GL11.glTranslated(0.0, 0.0, -0.35);
        ResourceHandler.bindResource("textures/particle/reactorBeam.png");
        drawBeam(tessellator, core, 1.0, 0.355, 0.8, offsetLength, partialTick, true, false, 0x00ffff);
        GL11.glPopMatrix();

        GL11.glPushMatrix();
        GL11.glTranslated(0.0, 0.0, 0.45);
        double coreSize = core.getCoreRadius() * 0.9;
        double scale = 0.355;
        drawBeam(
                tessellator,
                core,
                scale / coreSize,
                coreSize,
                offsetLength - core.getCoreDiameter() / 2.5,
                offsetLength,
                partialTick,
                false,
                false,
                0x00ffff);
        GL11.glPopMatrix();

        GL11.glPushMatrix();
        GL11.glTranslated(0.0, 0.0, -0.35);
        drawBeam(tessellator, core, 1.0, 0.263, 0.8, offsetLength, partialTick, true, true, 0xff6600);
        GL11.glPopMatrix();

        GL11.glPushMatrix();
        GL11.glTranslated(0.0, 0.0, 0.45);
        coreSize = core.getCoreRadius() * 0.4;
        scale = 0.263;
        drawBeam(
                tessellator,
                core,
                scale / coreSize,
                coreSize,
                offsetLength - 0.5,
                offsetLength,
                partialTick,
                false,
                true,
                0xff6600);
        GL11.glPopMatrix();

        GL11.glShadeModel(GL11.GL_FLAT);
    }

    /**
     * Size Origin is the fraction of size start Target So if size target is 10 and size origin is 0.5 origin will
     * actually be 5
     */
    private void drawBeam(Tessellator tessellator, TileReactorCore core, double sizeOrigin, double sizeTarget,
            double length, double offsetLength, float partialTick, boolean reverseTransparency,
            boolean reverseDirection, int color) {
        final double speed = 3.0;
        final double tickOffset = ((double) particleAge + partialTick) * (reverseDirection ? -0.01 : 0.01) * speed;

        double texV1 = offsetLength / 32.0 + tickOffset;
        double texV2 = -0.1 + tickOffset;

        tessellator.startDrawing(GL11.GL_TRIANGLE_STRIP);
        tessellator.setBrightness(200);

        final int raysCount = 16;
        for (int ray = 0; ray <= raysCount; ++ray) {
            double texU = (double) ray / raysCount;
            double verX = Math.sin(texU * Math.PI * 2.13325) * sizeTarget;
            double verY = Math.cos(texU * Math.PI * 2.13325) * sizeTarget;
            tessellator.setColorRGBA(
                    (color & 0xFF0000) >> 16,
                    (color & 0xFF00) >> 8,
                    color & 0xFF,
                    reverseTransparency ? 0 : (int) (255.0 * core.renderSpeed));
            tessellator.addVertexWithUV(verX * sizeOrigin, verY * sizeOrigin, 0.0, texU, texV1);
            tessellator.setColorRGBA(
                    (color & 0xFF0000) >> 16,
                    (color & 0xFF00) >> 8,
                    color & 0xFF,
                    reverseTransparency ? (int) (255.0 * core.renderSpeed) : 0);
            tessellator.addVertexWithUV(verX, verY, length, texU, texV2);
        }
        tessellator.draw();
    }
}
