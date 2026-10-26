package com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot

import com.naosyth.dimensionalbackpacks.dimensional_backpacks.network.PlotColorPayload
import net.minecraft.server.level.ServerPlayer
import net.neoforged.neoforge.network.PacketDistributor

/** Tells clients the tint of a plot's interior walls, which the client can't derive from the world. */
object PlotColorSync {
    fun send(player: ServerPlayer, plot: Plot) {
        PacketDistributor.sendToPlayer(player, PlotColorPayload(plot.id, plot.color ?: PlotColorPayload.NONE))
    }
}
