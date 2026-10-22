package com.naosyth.dimensionalbackpacks.dimensional_backpacks.block

import com.mojang.serialization.MapCodec
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.PlotManager
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties

/**
 * Placed against an interior wall of a plot. Its keybind right-clicks the block it faces, which is
 * the block touching the hotspot on the side away from the wall.
 */
class HotspotBlock(properties: Properties) : Block(properties) {
    init {
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH))
    }

    override fun codec(): MapCodec<out Block> = CODEC

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        val level = context.level
        if (level.dimension() != PlotManager.DIMENSION) {
            context.player?.displayClientMessage(Component.literal("Hotspots only work inside a backpack"), true)
            return null
        }
        val facing = context.clickedFace
        val pos = context.clickedPos
        if (!level.getBlockState(pos.relative(facing.opposite)).`is`(ModBlocks.PLOT_WALL.get())) {
            context.player?.displayClientMessage(Component.literal("Place hotspots against a backpack wall"), true)
            return null
        }
        // The registry only exists on the server; the client trusts the server to reject the placement.
        if (level is ServerLevel) {
            val manager = PlotManager.get(level)
            val plot = manager.plotAt(pos)
            if (plot == null || !manager.hasFreeHotspotSlot(plot)) {
                context.player?.displayClientMessage(Component.literal("This backpack has no free hotspot slots"), true)
                return null
            }
        }
        return defaultBlockState().setValue(FACING, facing)
    }

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, movedByPiston: Boolean) {
        super.onPlace(state, level, pos, oldState, movedByPiston)
        if (level !is ServerLevel || oldState.`is`(this)) return
        val manager = PlotManager.get(level)
        val plot = manager.plotAt(pos) ?: return
        manager.addHotspot(plot, pos)
    }

    // Right-clicking with an empty hand marks this hotspot as the primary one.
    override fun useWithoutItem(state: BlockState, level: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        if (!player.mainHandItem.isEmpty) return InteractionResult.PASS
        if (level is ServerLevel) {
            val manager = PlotManager.get(level)
            manager.plotAt(pos)?.let { manager.setPrimary(it, pos) }
            player.displayClientMessage(Component.literal("Primary hotspot set"), true)
        }
        return InteractionResult.sidedSuccess(level.isClientSide)
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, movedByPiston: Boolean) {
        if (level is ServerLevel && !newState.`is`(this)) {
            val manager = PlotManager.get(level)
            manager.plotAt(pos)?.let { manager.removeHotspot(it, pos) }
        }
        super.onRemove(state, level, pos, newState, movedByPiston)
    }

    companion object {
        val CODEC: MapCodec<HotspotBlock> = simpleCodec(::HotspotBlock)
        val FACING = BlockStateProperties.FACING
    }
}
