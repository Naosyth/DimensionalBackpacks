package com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot

import net.minecraft.server.MinecraftServer

/**
 * Keeps plot interiors loaded and ticking while a backpack is worn or placed.
 *
 * A dimension with no players stops ticking after 15 seconds unless it has forced chunks, so each
 * active plot's chunk is force-loaded. Owners call [keepLoaded] regularly; plots that stop being
 * refreshed are released, which pauses their interior.
 */
object PlotLoader {
    const val REFRESH_INTERVAL = 20
    private const val EXPIRY_TICKS = REFRESH_INTERVAL * 2L

    private val lastRefresh = HashMap<Int, Long>()

    fun keepLoaded(server: MinecraftServer, plot: Plot) {
        val pocket = PlotManager.getPocketLevel(server) ?: return
        forEachChunk(plot) { x, z -> pocket.setChunkForced(x, z, true) }
        lastRefresh[plot.id] = server.tickCount.toLong()
    }

    // Plot origins are chunk-aligned, but a plot can span several chunks.
    private fun forEachChunk(plot: Plot, action: (Int, Int) -> Unit) {
        val o = plot.origin
        val l = plot.tier.layout
        for (x in (o.x shr 4)..((o.x + l.sizeX - 1) shr 4)) {
            for (z in (o.z shr 4)..((o.z + l.sizeZ - 1) shr 4)) action(x, z)
        }
    }

    fun tick(server: MinecraftServer) {
        if (server.tickCount % REFRESH_INTERVAL != 0 || lastRefresh.isEmpty()) return
        val pocket = PlotManager.getPocketLevel(server) ?: return
        val manager = PlotManager.get(pocket)
        val expired = lastRefresh.filterValues { server.tickCount - it > EXPIRY_TICKS }.keys
        for (id in expired) {
            lastRefresh.remove(id)
            val plot = manager.get(id) ?: continue
            forEachChunk(plot) { x, z -> pocket.setChunkForced(x, z, false) }
        }
    }

    /** Forced chunks are saved with the level, so release any left over from a previous session. */
    fun releaseAll(server: MinecraftServer) {
        val pocket = PlotManager.getPocketLevel(server) ?: return
        lastRefresh.clear()
        for (packed in pocket.forcedChunks.toLongArray()) {
            pocket.setChunkForced(net.minecraft.world.level.ChunkPos.getX(packed), net.minecraft.world.level.ChunkPos.getZ(packed), false)
        }
    }

    fun activePlotIds(): Set<Int> = lastRefresh.keys.toSet()
}
