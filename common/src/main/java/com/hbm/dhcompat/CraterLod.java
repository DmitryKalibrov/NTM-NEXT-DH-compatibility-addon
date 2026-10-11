package com.hbm.dhcompat;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Server: DH re-reads a chunk only when it is saved to disk, so a fresh crater stayed intact on the
 * LOD horizon until autosave/unload (or forever, from the client's view, on a server). Once NTM is
 * done with a blast area, hand its loaded chunks to DH directly. Only touched when DH is loaded.
 */
public final class CraterLod {

    /** Ticks to wait so NTM's relighting settles before DH reads the chunks. */
    private static final int DELAY = 100;

    private record Job(ServerLevel level, int x, int z, int radius, long due) {}

    private static final List<Job> JOBS = new ArrayList<>();

    private CraterLod() {}

    /** End of every server tick, while DH is loaded. */
    public static void tick() {
        JOBS.removeIf(j -> j.level.getGameTime() >= j.due && run(j));
    }

    public static void queue(Entity e, int radius) {
        if (e.level() instanceof ServerLevel level)
            JOBS.add(new Job(level, e.getBlockX(), e.getBlockZ(), radius, level.getGameTime() + DELAY));
    }

    private static boolean run(Job j) {
        if (!DhApi.Delayed.worldProxy.worldLoaded()) return true;
        IDhApiLevelWrapper wrapper = null;
        for (IDhApiLevelWrapper w : DhApi.Delayed.worldProxy.getAllLoadedLevelWrappers())
            if (w.getWrappedMcObject() == j.level) wrapper = w;
        if (wrapper == null) return true;

        int r = (j.radius >> 4) + 1, cx = j.x >> 4, cz = j.z >> 4, sent = 0;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r) continue;
                // Unloaded chunks were saved on unload, which DH already picked up.
                LevelChunk chunk = j.level.getChunkSource().getChunkNow(cx + dx, cz + dz);
                if (chunk == null) continue;
                DhApi.Delayed.terrainRepo.overwriteChunkDataAsync(wrapper, new Object[] {chunk, j.level});
                sent++;
            }
        }
        DistantFx.LOG.debug("Sent {} crater chunks around {} {} to DH", sent, j.x, j.z);
        return true;
    }
}
