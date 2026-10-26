package com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot

import com.naosyth.dimensionalbackpacks.dimensional_backpacks.item.BackpackItem
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.item.ModComponents
import net.minecraft.server.level.ServerPlayer
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.config.ModConfig
import net.minecraft.network.chat.Component
import net.minecraft.world.level.Level
import net.neoforged.neoforge.event.level.BlockEvent
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.item.WornBackpack
import net.neoforged.neoforge.event.server.ServerStartedEvent
import net.neoforged.neoforge.event.tick.PlayerTickEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent

object BackpackEvents {
    fun onPlayerTick(event: PlayerTickEvent.Post) {
        val player = event.entity as? ServerPlayer ?: return
        if (player.tickCount % PlotLoader.REFRESH_INTERVAL != 0) return
        val stack = WornBackpack.find(player)
        val item = stack.item as? BackpackItem ?: return

        var id = stack.get(ModComponents.PLOT_ID.get())
        if (id == null) {
            // A backpack that was never placed gets its interior the first time it is worn.
            id = PlotManager.allocate(player.server, item.tier)?.id ?: return
            stack.set(ModComponents.PLOT_ID.get(), id)
        }
        val pocket = PlotManager.getPocketLevel(player.server) ?: return
        val plot = PlotManager.get(pocket).get(id) ?: return
        PlotLoader.keepLoaded(player.server, plot)
    }

    fun onServerTick(event: ServerTickEvent.Post) = PlotLoader.tick(event.server)

    fun onServerStarted(event: ServerStartedEvent) = PlotLoader.releaseAll(event.server)

    /** Enforces the placement blacklist inside backpack interiors. */
    fun onBlockPlaced(event: BlockEvent.EntityPlaceEvent) {
        val level = event.level as? Level ?: return
        if (level.isClientSide || level.dimension() != PlotManager.DIMENSION) return
        if (!ModConfig.isPlacementBlocked(event.placedBlock)) return
        event.isCanceled = true
        (event.entity as? ServerPlayer)?.displayClientMessage(Component.literal("That block can't be placed inside a backpack"), true)
    }
}
