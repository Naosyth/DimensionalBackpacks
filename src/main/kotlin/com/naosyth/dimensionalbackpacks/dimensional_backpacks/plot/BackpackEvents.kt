package com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot

import com.naosyth.dimensionalbackpacks.dimensional_backpacks.item.BackpackItem
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.item.ModComponents
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.EquipmentSlot
import net.neoforged.neoforge.event.server.ServerStartedEvent
import net.neoforged.neoforge.event.tick.PlayerTickEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent

object BackpackEvents {
    fun onPlayerTick(event: PlayerTickEvent.Post) {
        val player = event.entity as? ServerPlayer ?: return
        if (player.tickCount % PlotLoader.REFRESH_INTERVAL != 0) return
        val stack = player.getItemBySlot(EquipmentSlot.CHEST)
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
}
