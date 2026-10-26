package com.naosyth.dimensionalbackpacks.dimensional_backpacks.item

import com.naosyth.dimensionalbackpacks.dimensional_backpacks.block.PlotWallBlock
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.PlotManager
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.Item
import net.minecraft.world.item.context.UseOnContext

/** Right-click an interior wall face to put a hotspot decal on it. A wall block can hold one per face. */
class HotspotItem(properties: Properties) : Item(properties) {
    override fun useOn(context: UseOnContext): InteractionResult {
        val level = context.level
        val pos = context.clickedPos
        val face = context.clickedFace
        val player = context.player
        val state = level.getBlockState(pos)

        if (level.dimension() != PlotManager.DIMENSION || state.block !is PlotWallBlock) {
            player?.displayClientMessage(Component.literal("Use hotspots on a backpack's interior wall"), true)
            return InteractionResult.FAIL
        }
        if (PlotWallBlock.hasHotspot(state, face)) return InteractionResult.FAIL
        if (level !is ServerLevel) return InteractionResult.SUCCESS

        // The clicked face must look into the room, not out of the plot or into another wall block.
        val manager = PlotManager.get(level)
        val plot = manager.plotAt(pos)
        if (plot == null || !plot.isInterior(pos.relative(face))) return InteractionResult.FAIL
        if (!manager.hasFreeHotspotSlot(plot)) {
            player?.displayClientMessage(Component.literal("This backpack has no free hotspot slots"), true)
            return InteractionResult.FAIL
        }
        level.setBlock(pos, state.setValue(PlotWallBlock.FACE_PROPERTIES.getValue(face), true), 3)
        manager.addHotspot(plot, pos, face)
        if (player?.isCreative != true) context.itemInHand.shrink(1)
        return InteractionResult.CONSUME
    }
}
