package com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot

import com.naosyth.dimensionalbackpacks.dimensional_backpacks.block.PlotWallBlock
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.config.ModConfig
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.item.BackpackItem
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.item.ModComponents
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.ItemInteractionResult
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.item.WornBackpack
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.network.PacketDistributor
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.network.HotspotMenuPayload
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3

/** Performs a right-click on the block behind a hotspot of the backpack the player is wearing. */
object HotspotUse {
    const val MAX_SLOTS = ModConfig.MAX_HOTSPOTS
    const val PRIMARY = -1

    private fun wornPlot(player: ServerPlayer): Plot? {
        val worn = WornBackpack.find(player)
        if (worn.item !is BackpackItem) return notify(player, "You aren't wearing a backpack")
        val pocket = PlotManager.getPocketLevel(player.server) ?: return null
        return worn.get(ModComponents.PLOT_ID.get())?.let { PlotManager.get(pocket).get(it) }
    }

    private fun notify(player: ServerPlayer, message: String): Nothing? {
        player.displayClientMessage(Component.literal(message), true)
        return null
    }

    /** Answers a hold of the hotkey with the hotspots available to pick from. */
    fun sendMenu(player: ServerPlayer) {
        if (player.containerMenu !== player.inventoryMenu) return
        val plot = wornPlot(player) ?: return
        if (plot.hotspots.isEmpty()) return notifyUnit(player, "This backpack has no hotspots")
        val pocket = PlotManager.getPocketLevel(player.server) ?: return
        PlotLoader.keepLoaded(player.server, plot)
        val primary = plot.primarySlot()
        val entries = plot.hotspots.toSortedMap().mapNotNull { (slot, hotspot) ->
            if (slot >= plot.tier.maxHotspots) return@mapNotNull null
            if (!PlotWallBlock.hasHotspot(pocket.getBlockState(hotspot.pos), hotspot.face)) return@mapNotNull null
            val targetPos = hotspot.pos.relative(hotspot.face)
            val target = pocket.getBlockState(targetPos)
            if (ModConfig.isHotspotBlocked(target)) return@mapNotNull null
            val icon = if (target.isAir) ItemStack.EMPTY else target.getCloneItemStack(hitFor(hotspot), pocket, targetPos, player)
            HotspotMenuPayload.Entry(slot, icon, slot == primary)
        }
        PacketDistributor.sendToPlayer(player, HotspotMenuPayload(entries))
    }

    private fun notifyUnit(player: ServerPlayer, message: String) {
        notify(player, message)
    }

    /**
     * The click a player standing at the hotspot and looking straight at the block would make: it lands
     * on the target's face next to the wall, so multipart blocks pick the part on that side.
     */
    private fun hitFor(hotspot: Hotspot): BlockHitResult {
        val targetPos = hotspot.pos.relative(hotspot.face)
        val location = Vec3.atCenterOf(targetPos).relative(hotspot.face.opposite, 0.5)
        return BlockHitResult(location, hotspot.face.opposite, targetPos, false)
    }

    /** Marks the hotspot in [slot] as the plot's primary. */
    fun setPrimary(player: ServerPlayer, slot: Int) {
        val plot = wornPlot(player) ?: return
        val hotspot = plot.hotspots[slot] ?: return
        val pocket = PlotManager.getPocketLevel(player.server) ?: return
        PlotManager.get(pocket).setPrimary(plot, hotspot.pos, hotspot.face)
        notify(player, "Primary hotspot set")
    }

    /** Uses the hotspot in [requestedSlot], or the primary one for [PRIMARY]. */
    fun use(player: ServerPlayer, requestedSlot: Int) {
        if (requestedSlot != PRIMARY && requestedSlot !in 0 until MAX_SLOTS) return
        if (player.containerMenu !== player.inventoryMenu) return
        val plot = wornPlot(player) ?: return
        val server = player.server
        val pocket = PlotManager.getPocketLevel(server) ?: return
        val slot = if (requestedSlot == PRIMARY) plot.primarySlot() else requestedSlot
        if (slot == null) return notifyUnit(player, "This backpack has no hotspots")
        val hotspot = plot.hotspots[slot]?.takeIf { slot < plot.tier.maxHotspots } ?: return notifyUnit(player, "That hotspot isn't set up")

        if (!PlotWallBlock.hasHotspot(pocket.getBlockState(hotspot.pos), hotspot.face)) return notifyUnit(player, "That hotspot isn't set up")

        // The plot may have been unloaded; make sure the target exists before using it.
        PlotLoader.keepLoaded(server, plot)
        val facing = hotspot.face
        val targetPos = hotspot.pos.relative(facing)
        val target = pocket.getBlockState(targetPos)
        if (target.isAir) return notifyUnit(player, "Nothing is touching that hotspot")
        if (ModConfig.isHotspotBlocked(target)) return notifyUnit(player, "That block can't be used through a hotspot")

        val hit = hitFor(hotspot)
        val hand = InteractionHand.MAIN_HAND
        // Not ServerPlayerGameMode.useItemOn: its item-use path builds a context from player.level(),
        // which would act on the player's own dimension at pocket coordinates.
        val itemResult = target.useItemOn(player.getItemInHand(hand), pocket, player, hand, hit)
        if (!itemResult.consumesAction() && itemResult == ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) {
            target.useWithoutItem(pocket, player, hit)
        }
        HotspotSessions.start(player, plot)
    }
}
