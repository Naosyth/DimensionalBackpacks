package com.naosyth.dimensionalbackpacks.dimensional_backpacks.api

import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.HotspotSessions
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player

/**
 * Entry point for other mods that want their blocks to work through hotspots.
 *
 * Hotspot use runs the block's normal right-click in the pocket dimension while the player is
 * elsewhere, so checks such as "is the player in the same dimension / within reach" fail. A mod's
 * own range check can call [isUsingHotspot] to allow those cases. Listen for [HotspotUseEvent] to
 * veto a use. Server side only.
 */
object HotspotApi {
    /**
     * True while [player] is using a hotspot targeting [pos], and while a menu opened through a
     * hotspot on a plot containing [pos] is still open.
     */
    @JvmStatic
    fun isUsingHotspot(player: Player, pos: BlockPos): Boolean = HotspotSessions.isUsing(player, pos)
}
