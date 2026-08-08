package com.brandon3055.draconicevolution.client.render.tile;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.ITickable;
import net.minecraft.item.ItemStack;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.event.RenderWorldLastEvent;
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

public class PlacedItemDisplayListCache {

    private static final long GLINT_TTL_MS = 33L;
    private static final long ANIMATED_TTL_MS = 50L;
    private static final long MOBSOUL_TTL_MS = 1000L;
    private static final long PRUNE_INTERVAL_MS = 1000L;

    private final Map<TilePlacedItem, CacheEntry> displayListCache = new HashMap<>();
    private int atlasVersion;
    private long nextPruneMs;

    public PlacedItemDisplayListCache() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    public CacheEntry getOrCompile(TilePlacedItem tile, ItemStack stack, int meta, Runnable renderBody) {
        CacheEntry entry = displayListCache.get(tile);
        if (entry == null || !entry.isValid(stack, meta, tile.rotation, atlasVersion) || entry.needsRefresh()) {
            dispose(tile);
            entry = compile(tile, stack, meta, renderBody);
        }
        return entry;
    }

    private CacheEntry compile(TilePlacedItem tile, ItemStack stack, int meta, Runnable renderBody) {
        int listId = GL11.glGenLists(1);
        boolean mobSoul = stack.getItem() instanceof MobSoul;
        boolean previousRotationFlag = RenderMobSoul.applyTimeRotation;
        GL11.glNewList(listId, GL11.GL_COMPILE);
        try {
            if (mobSoul) RenderMobSoul.applyTimeRotation = false;
            renderBody.run();
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
                getTTL(stack, mobSoul),
                mobSoul,
                atlasVersion);
        displayListCache.put(tile, entry);
        return entry;
    }

    private long getTTL(ItemStack stack, boolean mobSoul) {
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

    public void dispose(TilePlacedItem tile) {
        CacheEntry entry = displayListCache.remove(tile);
        if (entry != null) GL11.glDeleteLists(entry.listId, 1);
    }

    private void pruneInvalidEntries() {
        if (displayListCache.isEmpty()) return;
        long now = Minecraft.getSystemTime();
        if (now < nextPruneMs) return;
        nextPruneMs = now + PRUNE_INTERVAL_MS;

        Iterator<Map.Entry<TilePlacedItem, CacheEntry>> it = displayListCache.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<TilePlacedItem, CacheEntry> entry = it.next();
            TilePlacedItem tile = entry.getKey();
            if (tile.isInvalid() || tile.getWorldObj() == null || tile.getStack() == null) {
                GL11.glDeleteLists(entry.getValue().listId, 1);
                it.remove();
            }
        }
    }

    private void clearAll() {
        for (CacheEntry entry : displayListCache.values()) {
            GL11.glDeleteLists(entry.listId, 1);
        }
        displayListCache.clear();
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        pruneInvalidEntries();
    }

    @SubscribeEvent
    public void onTextureStitch(TextureStitchEvent.Post event) {
        atlasVersion++;
        clearAll();
    }

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        if (!event.world.isRemote) return;
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
        if (!event.world.isRemote) return;
        clearAll();
    }

    public static class CacheEntry {

        public final int listId;
        public final ItemStack stack;
        public final int stackSize;
        public final int meta;
        public final float rotation;
        public final long ttlMs;
        public final boolean mobSoul;
        public final int atlasVersion;
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
