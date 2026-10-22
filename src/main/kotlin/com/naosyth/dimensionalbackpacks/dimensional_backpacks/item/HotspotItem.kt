package com.naosyth.dimensionalbackpacks.dimensional_backpacks.item

import com.naosyth.dimensionalbackpacks.dimensional_backpacks.block.HotspotBlock
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.block.ModBlocks
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.PlotManager
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.Item
import net.minecraft.world.item.context.UseOnContext

/** Right-click an interior wall face to turn that wall block into a hotspot facing the room. */
class HotspotItem(properties: Properties) : Item(properties) {
    override fun useOn(context: UseOnContext): InteractionResult {
        val level = context.level
        val pos = context.clickedPos
        val face = context.clickedFace
        val player = context.player

        if (level.dimension() != PlotManager.DIMENSION || !level.getBlockState(pos).`is`(ModBlocks.PLOT_WALL.get())) {
            player?.displayClientMessage(Component.literal("Use hotspots on a backpack's interior wall"), true)
            return InteractionResult.FAIL
        }
        // The clicked face must look into the room, not out of the plot or into another wall block.
        val inside = pos.relative(face)
        if (level.getBlockState(inside).`is`(ModBlocks.PLOT_WALL.get())) return InteractionResult.FAIL
        if (level !is ServerLevel) return InteractionResult.SUCCESS

        val manager = PlotManager.get(level)
        val plot = manager.plotAt(pos)
        if (plot == null || !plot.contains(inside)) return InteractionResult.FAIL
        if (!manager.hasFreeHotspotSlot(plot)) {
            player?.displayClientMessage(Component.literal("This backpack has no free hotspot slots"), true)
            return InteractionResult.FAIL
        }
        level.setBlock(pos, ModBlocks.HOTSPOT.get().defaultBlockState().setValue(HotspotBlock.FACING, face), 3)
        if (player?.isCreative != true) context.itemInHand.shrink(1)
        return InteractionResult.CONSUME
    }
}
