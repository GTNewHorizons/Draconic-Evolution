package com.brandon3055.draconicevolution.client.render.tile;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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

    public CacheEntry getOrCompile(TilePlacedItem tile, int meta, Runnable renderBody) {
        List<ItemStack> stacks = tile.getStacks();
        CacheEntry entry = displayListCache.get(tile);
        if (entry == null || !entry.isValid(stacks, meta, tile.rotation, atlasVersion) || entry.needsRefresh()) {
            dispose(tile);
            entry = compile(tile, stacks, meta, renderBody);
        }
        return entry;
    }

    private CacheEntry compile(TilePlacedItem tile, List<ItemStack> stacks, int meta, Runnable renderBody) {
        int listId = GL11.glGenLists(1);
        // A lone mob soul sits at the block centre, so its spin can be applied outside the list each frame. Souls in
        // a grid are baked with their current angle instead and the list is refreshed on a short TTL.
        boolean mobSoul = stacks.size() == 1 && stacks.get(0).getItem() instanceof MobSoul;
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
                stacks,
                meta,
                tile.rotation,
                getTTL(stacks, mobSoul),
                mobSoul,
                atlasVersion);
        displayListCache.put(tile, entry);
        return entry;
    }

    private long getTTL(List<ItemStack> stacks, boolean mobSoulOutsideList) {
        long ttl = Long.MAX_VALUE; // static (event-invalidated)
        for (ItemStack stack : stacks) {
            ttl = Math.min(ttl, getTTL(stack, mobSoulOutsideList));
        }
        return ttl;
    }

    private long getTTL(ItemStack stack, boolean mobSoulOutsideList) {
        if (stack.getItem() instanceof MobSoul) {
            // "Any" soul swaps mob every second while named souls only need per-frame rotation outside the list
            if (isAnyMobSoul(stack)) return MOBSOUL_TTL_MS;
            return mobSoulOutsideList ? Long.MAX_VALUE : ANIMATED_TTL_MS;
        }

        if (stack.hasEffect()) return GLINT_TTL_MS; // Glint scroll (~30 fps)
        if (stack.getItem().getIconIndex(stack) instanceof ITickable) return ANIMATED_TTL_MS; // 20 Hz sprite ticks
        return Long.MAX_VALUE;
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
            if (tile.isInvalid() || tile.getWorldObj() == null || tile.getDisplayCount() == 0) {
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
        private final ItemStack[] stacks;
        private final int[] stackSizes;
        public final int meta;
        public final float rotation;
        public final long ttlMs;
        /** True when the list holds a single mob soul whose spin is applied by the caller each frame. */
        public final boolean mobSoul;
        public final int atlasVersion;
        private long lastCompileMs;

        private CacheEntry(int listId, List<ItemStack> stacks, int meta, float rotation, long ttlMs, boolean mobSoul,
                int atlasVersion) {
            this.listId = listId;
            this.stacks = stacks.toArray(new ItemStack[0]);
            this.stackSizes = new int[this.stacks.length];
            for (int i = 0; i < this.stacks.length; i++) {
                this.stackSizes[i] = this.stacks[i].stackSize;
            }
            this.meta = meta;
            this.rotation = rotation;
            this.ttlMs = ttlMs;
            this.mobSoul = mobSoul;
            this.atlasVersion = atlasVersion;
            this.lastCompileMs = System.currentTimeMillis();
        }

        private boolean isValid(List<ItemStack> stacks, int meta, float rotation, int atlasVersion) {
            if (this.meta != meta || this.rotation != rotation || this.atlasVersion != atlasVersion) return false;
            if (this.stacks.length != stacks.size()) return false;
            for (int i = 0; i < this.stacks.length; i++) {
                // checking the pointer of the stack is enough
                ItemStack stack = stacks.get(i);
                if (this.stacks[i] != stack || this.stackSizes[i] != stack.stackSize) return false;
            }
            return true;
        }

        private boolean needsRefresh() {
            return ttlMs != Long.MAX_VALUE && System.currentTimeMillis() - lastCompileMs >= ttlMs;
        }
    }
}
