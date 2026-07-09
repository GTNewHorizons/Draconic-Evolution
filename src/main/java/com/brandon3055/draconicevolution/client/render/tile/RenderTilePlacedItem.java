package com.brandon3055.draconicevolution.client.render.tile;

import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import org.lwjgl.opengl.GL11;

import com.brandon3055.draconicevolution.client.render.item.RenderMobSoul;
import com.brandon3055.draconicevolution.client.render.tile.PlacedItemDisplayListCache.CacheEntry;
import com.brandon3055.draconicevolution.common.items.MobSoul;
import com.brandon3055.draconicevolution.common.tileentities.TilePlacedItem;

/**
 * Created by Brandon on 27/07/2014.
 */
public class RenderTilePlacedItem extends TileEntitySpecialRenderer {

    private static final float MOBSOUL_CENTER_X = 0.5F;
    private static final float MOBSOUL_CENTER_Y = 0.3F;
    private static final float MOBSOUL_CENTER_Z = 0.5F;

    private final EntityItem cachedEntity = new EntityItem(null, 0, 0, 0, new ItemStack(Items.apple));
    private final PlacedItemDisplayListCache displayListCache = new PlacedItemDisplayListCache();

    /**
     * Pivot point on the mounting face and the two in-plane axes used to lay out the item grid, indexed by block
     * metadata (the side the display is attached to).
     */
    private static final float[][] FACE_PIVOT = { { 0.5F, 1F, 0.5F }, { 0.5F, 0F, 0.5F }, { 0.5F, 0.5F, 1F },
            { 0.5F, 0.5F, 0F }, { 1F, 0.5F, 0.5F }, { 0F, 0.5F, 0.5F } };
    private static final float[][] FACE_AXIS_U = { { 1F, 0F, 0F }, { 1F, 0F, 0F }, { 1F, 0F, 0F }, { 1F, 0F, 0F },
            { 0F, 0F, 1F }, { 0F, 0F, 1F } };
    private static final float[][] FACE_AXIS_V = { { 0F, 0F, 1F }, { 0F, 0F, 1F }, { 0F, 1F, 0F }, { 0F, 1F, 0F },
            { 0F, 1F, 0F }, { 0F, 1F, 0F } };

    @Override
    public void renderTileEntityAt(TileEntity te, double x, double y, double z, float timeSinceLastTick) {
        if (!(te instanceof TilePlacedItem tile)) return;
        int count = tile.getDisplayCount();
        if (count == 0) {
            displayListCache.dispose(tile);
            return;
        }
        if (tile.getWorldObj() == null) return;
        int meta = tile.getWorldObj().getBlockMetadata(tile.xCoord, tile.yCoord, tile.zCoord);
        if (meta < 0 || meta > 5) meta = 1;
        final int gridMeta = meta;
        CacheEntry entry = displayListCache.getOrCompile(tile, meta, () -> renderGrid(tile, gridMeta));

        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
        GL11.glTranslated(x, y, z);
        if (entry.mobSoul) {
            GL11.glTranslated(MOBSOUL_CENTER_X, MOBSOUL_CENTER_Y, MOBSOUL_CENTER_Z);
            GL11.glRotatef(RenderMobSoul.getRotationAngle(), 0F, 1F, 0F);
            GL11.glTranslated(-MOBSOUL_CENTER_X, -MOBSOUL_CENTER_Y, -MOBSOUL_CENTER_Z);
        }
        GL11.glCallList(entry.listId);
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    /**
     * Renders every item on the display laid out in a grid on the mounting face. This is the body that gets compiled
     * into the cached display list.
     */
    private void renderGrid(TilePlacedItem tile, int meta) {
        int count = tile.getDisplayCount();
        int gridSize = (int) Math.ceil(Math.sqrt(count));
        int rows = (count + gridSize - 1) / gridSize;
        float cell = 1F / gridSize;
        float[] pivot = FACE_PIVOT[meta];
        float[] axisU = FACE_AXIS_U[meta];
        float[] axisV = FACE_AXIS_V[meta];

        for (int i = 0; i < count; i++) {
            ItemStack stack = tile.getStack(i);
            if (stack == null) continue;

            GL11.glPushMatrix();
            if (gridSize > 1) {
                int row = i / gridSize;
                int col = i % gridSize;
                int rowLength = Math.min(gridSize, count - row * gridSize);
                float u = (col - (rowLength - 1) / 2F) * cell;
                float v = ((rows - 1) / 2F - row) * cell;

                GL11.glTranslatef(
                        pivot[0] + axisU[0] * u + axisV[0] * v,
                        pivot[1] + axisU[1] * u + axisV[1] * v,
                        pivot[2] + axisU[2] * u + axisV[2] * v);
                GL11.glScalef(cell, cell, cell);
                GL11.glTranslatef(-pivot[0], -pivot[1], -pivot[2]);
            }
            renderItem(tile, stack, meta);
            GL11.glPopMatrix();
        }
    }

    public void renderItem(TilePlacedItem tile, ItemStack stack, int meta) {
        final Item item = stack.getItem();
        boolean is3D = item.isFull3D();
        boolean isBlock = item instanceof ItemBlock;

        if (isBlock) {
            GL11.glTranslatef(0.5F, 0.25F, 0.5F);
            GL11.glScalef(1.5F, 1.5F, 1.5F);
            metaAdjustBlock(meta);
            GL11.glRotatef(tile.rotation, 0F, 1F, 0F);
        } else if (is3D || item instanceof ItemArmor || item instanceof ItemBow) {
            GL11.glScalef(2F, 2F, 2F);
            GL11.glRotatef(90F, -1F, 0F, 0F);
            GL11.glTranslatef(0.25F, -0.45F, 0.02F);
            metaAdjustItemTool(meta);
            GL11.glTranslatef(0.0F, 0.21F, 0.0F);
            GL11.glRotatef(tile.rotation, 0F, 0F, 1F);
            GL11.glTranslatef(0.0F, -0.21F, 0.0F);
        } else if (item instanceof MobSoul) {
            GL11.glTranslatef(MOBSOUL_CENTER_X, MOBSOUL_CENTER_Y, MOBSOUL_CENTER_Z);
        } else {
            GL11.glRotatef(90F, -1F, 0F, 0F);
            GL11.glTranslatef(0.5F, -0.65F, 0.02F);
            metaAdjustItem(meta);
            GL11.glTranslatef(0.0F, 0.18F, 0.0F);
            GL11.glRotatef(tile.rotation, 0F, 0F, 1F);
            GL11.glTranslatef(0.0F, -0.18F, 0.0F);
        }

        GL11.glPushAttrib(GL11.GL_COLOR_BUFFER_BIT);
        RenderItem.renderInFrame = true;
        try {
            this.cachedEntity.setEntityItemStack(stack);
            this.cachedEntity.setWorld(tile.getWorldObj());
            this.cachedEntity.hoverStart = 0.0F;
            RenderManager.instance.renderEntityWithPosYaw(this.cachedEntity, 0.0D, 0.0D, 0.0D, 0.0F, 0.0F);
        } finally {
            this.cachedEntity.setEntityItemStack(null);
            this.cachedEntity.setWorld(null);
        }
        RenderItem.renderInFrame = false;
        GL11.glPopAttrib();
    }

    private void metaAdjustItemTool(int meta) {
        switch (meta) {
            case 0:
                GL11.glRotatef(180F, 0F, 1F, 0F);
                GL11.glTranslatef(0.0F, 0.0F, -0.46F);
                break;
            case 1:
                break;
            case 2:
                GL11.glRotatef(90F, 1F, 0F, 0F);
                GL11.glRotatef(180F, 0F, 1F, 0F);
                GL11.glTranslatef(0.0F, 0.02F, -0.03F);
                break;
            case 3:
                GL11.glRotatef(90F, 1F, 0F, 0F);
                GL11.glTranslatef(0.0F, 0.02F, -0.43F);
                break;
            case 4:
                GL11.glRotatef(90F, 0F, -1F, 0F);
                GL11.glRotatef(90F, 0F, 0F, -1F);
                GL11.glTranslatef(-0.205F, 0.02F, -0.23F);
                break;
            case 5:
                GL11.glRotatef(90F, 0F, -1F, 0F);
                GL11.glRotatef(90F, 0F, 0F, -1F);
                GL11.glRotatef(180F, 0F, 1F, 0F);
                GL11.glTranslatef(0.223F, 0.0F, -0.23F);
                break;
        }
    }

    private void metaAdjustItem(int meta) {
        switch (meta) {
            case 0:
                GL11.glRotatef(180F, 0F, 1F, 0F);
                GL11.glTranslatef(0.0F, 0.0F, -0.96F);
                break;
            case 1:
                break;
            case 2:
                GL11.glRotatef(90F, 1F, 0F, 0F);
                GL11.glRotatef(180F, 0F, 1F, 0F);
                GL11.glTranslatef(0.0F, 0.32F, -0.33F);
                break;
            case 3:
                GL11.glRotatef(90F, 1F, 0F, 0F);
                GL11.glTranslatef(0.0F, 0.3F, -0.63F);
                break;
            case 4:
                GL11.glRotatef(90F, 0F, -1F, 0F);
                GL11.glRotatef(90F, 0F, 0F, -1F);
                GL11.glTranslatef(-0.17F, 0.302F, -0.475F);
                break;
            case 5:
                GL11.glRotatef(90F, 0F, -1F, 0F);
                GL11.glRotatef(90F, 0F, 0F, -1F);
                GL11.glRotatef(180F, 0F, 1F, 0F);
                GL11.glTranslatef(0.15F, 0.3F, -0.475F);
                break;
        }
    }

    private void metaAdjustBlock(int meta) {
        switch (meta) {
            case 0:
                GL11.glTranslatef(0.0F, 0.51F, 0.0F);
                GL11.glRotatef(180F, 0F, 0F, 1F);
                break;
            case 1:
                GL11.glTranslatef(0.0F, -0.17F, 0.0F);
                break;
            case 2:
                GL11.glTranslatef(-0.0F, 0.17F, 0.34F);
                GL11.glRotatef(90F, -1F, 0F, 0F);
                break;
            case 3:
                GL11.glTranslatef(0.0F, 0.17F, -0.34F);
                GL11.glRotatef(90F, 1F, 0F, 0F);
                break;
            case 4:
                GL11.glTranslatef(0.34F, 0.17F, 0.0F);
                GL11.glRotatef(90F, 0F, 0F, 1F);
                break;
            case 5:
                GL11.glTranslatef(-0.34F, 0.17F, 0.0F);
                GL11.glRotatef(90F, 0F, 0F, -1F);
                break;
        }
    }
}
