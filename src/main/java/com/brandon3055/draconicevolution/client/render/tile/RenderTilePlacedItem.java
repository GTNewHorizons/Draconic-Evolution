package com.brandon3055.draconicevolution.client.render.tile;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.ITickable;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;

import org.lwjgl.opengl.GL11;

import com.brandon3055.draconicevolution.client.render.item.RenderMobSoul;
import com.brandon3055.draconicevolution.common.items.MobSoul;
import com.brandon3055.draconicevolution.common.tileentities.TilePlacedItem;
import com.brandon3055.draconicevolution.common.utils.ItemNBTHelper;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Created by Brandon on 27/07/2014.
 */
public class RenderTilePlacedItem extends TileEntitySpecialRenderer {

    private static final long GLINT_TTL_MS = 33L;
    private static final long ANIMATED_TTL_MS = 50L;
    private static final long MOBSOUL_TTL_MS = 1000L;
    private static final float MOBSOUL_CENTER_X = 0.5F;
    private static final float MOBSOUL_CENTER_Y = 0.3F;
    private static final float MOBSOUL_CENTER_Z = 0.5F;

    private final EntityItem cachedEntity = new EntityItem(null, 0, 0, 0, new ItemStack(Items.apple));
    private final Map<TilePlacedItem, CacheEntry> displayListCache = new WeakHashMap<>();
    private int atlasVersion;

    public RenderTilePlacedItem() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void renderTileEntityAt(TileEntity te, double x, double y, double z, float timeSinceLastTick) {
        if (!(te instanceof TilePlacedItem tile)) return;
        ItemStack stack = tile.getStack();
        if (stack == null) {
            dispose(tile);
            return;
        }
        if (tile.getWorldObj() == null) return;
        int meta = tile.getWorldObj().getBlockMetadata(tile.xCoord, tile.yCoord, tile.zCoord);
        CacheEntry entry = displayListCache.get(tile);
        if (entry == null || !entry.isValid(stack, meta, tile.rotation, atlasVersion) || entry.needsRefresh()) {
            dispose(tile);
            entry = compile(tile, stack, meta);
        }
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
        GL11.glTranslated(x, y, z);
        if (entry.mobSoul) {
            GL11.glTranslated(MOBSOUL_CENTER_X, MOBSOUL_CENTER_Y, MOBSOUL_CENTER_Z);
            GL11.glRotatef(Minecraft.getSystemTime() / -10, 0F, 1F, 0F);
            GL11.glTranslated(-MOBSOUL_CENTER_X, -MOBSOUL_CENTER_Y, -MOBSOUL_CENTER_Z);
        }
        GL11.glCallList(entry.listId);
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    private CacheEntry compile(TilePlacedItem tile, ItemStack stack, int meta) {
        int listId = GL11.glGenLists(1);
        boolean mobSoul = stack.getItem() instanceof MobSoul;
        boolean previousRotationFlag = RenderMobSoul.applyTimeRotation;
        GL11.glNewList(listId, GL11.GL_COMPILE);
        try {
            if (mobSoul) RenderMobSoul.applyTimeRotation = false;
            renderItem(tile);
            GL11.glEndList();
        } catch (RuntimeException | Error t) {
            GL11.glEndList();
            GL11.glDeleteLists(listId, 1);
            RenderItem.renderInFrame = false;
            throw t;
        } finally {
            RenderMobSoul.applyTimeRotation = previousRotationFlag;
        }
        CacheEntry entry = new CacheEntry(
                listId,
                stack,
                stack.stackSize,
                meta,
                tile.rotation,
                getTtl(stack, mobSoul),
                mobSoul,
                atlasVersion);
        displayListCache.put(tile, entry);
        return entry;
    }

    private long getTtl(ItemStack stack, boolean mobSoul) {
        if (mobSoul) {
            // "Any" soul swaps mob every second while named souls only need per-frame rotation outside the list
            return isAnyMobSoul(stack) ? MOBSOUL_TTL_MS : Long.MAX_VALUE;
        }

        if (stack.hasEffect()) return GLINT_TTL_MS; // Glint scroll (~30 fps)
        if (stack.getItem().getIconIndex(stack) instanceof ITickable) return ANIMATED_TTL_MS; // 20 Hz sprite ticks
        return Long.MAX_VALUE; // static (event-invalidated)
    }

    private boolean isAnyMobSoul(ItemStack stack) {
        return "Any".equals(ItemNBTHelper.getString(stack, "Name", "Pig"));
    }

    private void dispose(TilePlacedItem tile) {
        CacheEntry entry = displayListCache.remove(tile);
        if (entry != null) GL11.glDeleteLists(entry.listId, 1);
    }

    private void clearAll() {
        for (CacheEntry entry : displayListCache.values()) {
            GL11.glDeleteLists(entry.listId, 1);
        }
        displayListCache.clear();
    }

    @SubscribeEvent
    public void onTextureStitch(TextureStitchEvent.Post event) {
        atlasVersion++;
        clearAll();
    }

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        Chunk chunk = event.getChunk();
        Iterator<Map.Entry<TilePlacedItem, CacheEntry>> it = displayListCache.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<TilePlacedItem, CacheEntry> e = it.next();
            TilePlacedItem tile = e.getKey();
            if ((tile.xCoord >> 4) == chunk.xPosition && (tile.zCoord >> 4) == chunk.zPosition) {
                GL11.glDeleteLists(e.getValue().listId, 1);
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        clearAll();
    }

    public void renderItem(TilePlacedItem tile) {
        ItemStack stack = tile.getStack();
        int meta = tile.getWorldObj().getBlockMetadata(tile.xCoord, tile.yCoord, tile.zCoord);
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

    private static class CacheEntry {

        private final int listId;
        private final ItemStack stack;
        private final int stackSize;
        private final int meta;
        private final float rotation;
        private final long ttlMs;
        private final boolean mobSoul;
        private final int atlasVersion;
        private long lastCompileMs;

        private CacheEntry(int listId, ItemStack stack, int stackSize, int meta, float rotation, long ttlMs,
                boolean mobSoul, int atlasVersion) {
            this.listId = listId;
            this.stack = stack;
            this.stackSize = stackSize;
            this.meta = meta;
            this.rotation = rotation;
            this.ttlMs = ttlMs;
            this.mobSoul = mobSoul;
            this.atlasVersion = atlasVersion;
            this.lastCompileMs = System.currentTimeMillis();
        }

        private boolean isValid(ItemStack stack, int meta, float rotation, int atlasVersion) {
            // checking the pointer of the stack is enough
            return this.stack == stack && this.stackSize == stack.stackSize
                    && this.meta == meta
                    && this.rotation == rotation
                    && this.atlasVersion == atlasVersion;
        }

        private boolean needsRefresh() {
            return ttlMs != Long.MAX_VALUE && System.currentTimeMillis() - lastCompileMs >= ttlMs;
        }
    }
}
