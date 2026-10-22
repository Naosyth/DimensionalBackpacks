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
