package com.naosyth.dimensionalbackpacks.dimensional_backpacks.block

import com.mojang.serialization.MapCodec
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.item.ModItems
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.PlotManager
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.phys.BlockHitResult

/**
 * An unbreakable wall block of a plot. Each of its six faces can carry a hotspot decal, so a corner
 * block can hold several. A face with a hotspot points into the room, and the block touching the
 * hotspot is the one at pos.relative(face).
 */
class PlotWallBlock(properties: Properties) : Block(properties) {
    init {
        var state = stateDefinition.any()
        for (property in FACE_PROPERTIES.values) state = state.setValue(property, false)
        registerDefaultState(state)
    }

    override fun codec(): MapCodec<out Block> = CODEC

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        FACE_PROPERTIES.values.forEach { builder.add(it) }
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, movedByPiston: Boolean) {
        if (level is ServerLevel && !newState.`is`(this)) {
            val manager = PlotManager.get(level)
            manager.plotAt(pos)?.let { manager.removeHotspotsAt(it, pos) }
            // Whatever removed the wall (e.g. creative mode), it must never be left with a hole.
            if (newState.isAir) level.setBlock(pos, defaultBlockState(), 3)
        }
        super.onRemove(state, level, pos, newState, movedByPiston)
    }

    override fun useWithoutItem(state: BlockState, level: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        val face = hit.direction
        if (!hasHotspot(state, face) || !player.mainHandItem.isEmpty) return InteractionResult.PASS
        if (level is ServerLevel) {
            val manager = PlotManager.get(level)
            val plot = manager.plotAt(pos)
            if (player.isShiftKeyDown) {
                // Sneak with an empty hand takes the hotspot back off the wall.
                level.setBlock(pos, state.setValue(FACE_PROPERTIES.getValue(face), false), 3)
                plot?.let { manager.removeHotspot(it, pos, face) }
                val item = ItemStack(ModItems.HOTSPOT.get())
                if (!player.inventory.add(item)) {
                    level.addFreshEntity(ItemEntity(level, player.x, player.y, player.z, item))
                }
            } else {
                // Otherwise an empty hand marks this hotspot as the primary one.
                plot?.let { manager.setPrimary(it, pos, face) }
                player.displayClientMessage(Component.literal("Primary hotspot set"), true)
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide)
    }

    companion object {
        val CODEC: MapCodec<PlotWallBlock> = simpleCodec(::PlotWallBlock)

        val FACE_PROPERTIES: Map<Direction, BooleanProperty> = linkedMapOf(
            Direction.NORTH to BlockStateProperties.NORTH,
            Direction.EAST to BlockStateProperties.EAST,
            Direction.SOUTH to BlockStateProperties.SOUTH,
            Direction.WEST to BlockStateProperties.WEST,
            Direction.UP to BlockStateProperties.UP,
            Direction.DOWN to BlockStateProperties.DOWN
        )

        fun hasHotspot(state: BlockState, face: Direction): Boolean =
            state.block is PlotWallBlock && state.getValue(FACE_PROPERTIES.getValue(face))
    }
}
