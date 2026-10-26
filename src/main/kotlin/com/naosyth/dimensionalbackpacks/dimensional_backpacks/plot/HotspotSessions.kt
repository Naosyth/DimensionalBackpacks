package com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import java.util.UUID

/**
 * Tracks menus a player opened through a hotspot. Those menus are far from the player (in another
 * dimension), so vanilla's distance check would close them immediately; while a session is active,
 * [allowsInteraction] lets that check pass for blocks inside the plot the menu came from.
 */
object HotspotSessions {
    private class Session(val plot: Plot, val menu: AbstractContainerMenu)

    private val sessions = HashMap<UUID, Session>()
    private val inProgress = HashMap<UUID, Pair<Plot, BlockPos>>()

    /** Marks the player as mid-use of a hotspot on [pos], so mod checks made while the block opens its menu can pass. */
    inline fun <T> during(player: ServerPlayer, plot: Plot, pos: BlockPos, block: () -> T): T {
        begin(player, plot, pos)
        try {
            return block()
        } finally {
            finish(player)
        }
    }

    fun begin(player: ServerPlayer, plot: Plot, pos: BlockPos) {
        inProgress[player.uuid] = plot to pos
    }

    fun finish(player: ServerPlayer) {
        inProgress.remove(player.uuid)
    }

    @JvmStatic
    fun isUsing(player: Player, pos: BlockPos): Boolean {
        if (player !is ServerPlayer) return false
        inProgress[player.uuid]?.let { (plot, target) -> if (target == pos || plot.contains(pos)) return true }
        return allowsInteraction(player, pos)
    }

    fun start(player: ServerPlayer, plot: Plot) {
        if (player.containerMenu !== player.inventoryMenu) sessions[player.uuid] = Session(plot, player.containerMenu)
    }

    fun end(player: Player) {
        sessions.remove(player.uuid)
    }

    /** Called from a mixin on `Player.canInteractWithBlock`. */
    @JvmStatic
    fun allowsInteraction(player: Player, pos: BlockPos): Boolean {
        if (sessions.isEmpty() || player !is ServerPlayer) return false
        val session = sessions[player.uuid] ?: return false
        if (player.containerMenu !== session.menu) {
            sessions.remove(player.uuid)
            return false
        }
        return session.plot.contains(pos)
    }
}
