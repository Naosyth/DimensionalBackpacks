package com.naosyth.dimensionalbackpacks.dimensional_backpacks.item

import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.PlotManager
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.PlotTravel
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.Spawn
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level

/** Used inside a backpack: right-click to leave, sneak + right-click to set where everyone appears on entering. */
class ZipperItem(properties: Properties) : Item(properties) {
    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(hand)
        if (level.dimension() != PlotManager.DIMENSION) {
            if (!level.isClientSide) player.displayClientMessage(Component.literal("Use this inside a backpack"), true)
            return InteractionResultHolder.fail(stack)
        }
        if (level !is ServerLevel || player !is ServerPlayer) return InteractionResultHolder.success(stack)

        if (player.isShiftKeyDown) setSpawn(level, player) else PlotTravel.exit(player)
        return InteractionResultHolder.success(stack)
    }

    private fun setSpawn(level: ServerLevel, player: ServerPlayer) {
        val manager = PlotManager.get(level)
        val pos = player.blockPosition()
        val plot = manager.plotAt(pos)
        if (plot == null || !plot.isInterior(pos) || !plot.isInterior(pos.above())) {
            player.displayClientMessage(Component.literal("Can't set the entry point here"), true)
            return
        }
        manager.setSpawn(plot, Spawn(player.x, player.y, player.z, player.yRot, player.xRot))
        player.displayClientMessage(Component.literal("Entry point set"), true)
    }

    override fun appendHoverText(
        stack: ItemStack, context: TooltipContext, tooltip: MutableList<Component>, flag: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltip, flag)
        tooltip.add(Component.literal("Right-click inside a backpack to leave"))
        tooltip.add(Component.literal("Sneak + right-click to set the entry point"))
    }
}
