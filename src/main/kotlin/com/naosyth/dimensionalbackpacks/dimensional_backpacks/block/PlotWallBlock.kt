package com.naosyth.dimensionalbackpacks.dimensional_backpacks.block

import com.mojang.serialization.MapCodec
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState

/** An unbreakable wall block of a plot. */
class PlotWallBlock(properties: Properties) : Block(properties) {
    override fun codec(): MapCodec<out Block> = CODEC

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, movedByPiston: Boolean) {
        // Whatever removed the wall (e.g. creative mode), it must never be left with a hole.
        if (level is ServerLevel && !newState.`is`(this) && newState.isAir) {
            level.setBlock(pos, defaultBlockState(), 3)
        }
        super.onRemove(state, level, pos, newState, movedByPiston)
    }

    companion object {
        val CODEC: MapCodec<PlotWallBlock> = simpleCodec(::PlotWallBlock)
    }
}
