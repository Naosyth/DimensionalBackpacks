package com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot

import com.naosyth.dimensionalbackpacks.dimensional_backpacks.block.ModBlocks
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.Blocks

object PlotBuilder {
    /** Builds a hollow room: unbreakable walls around an empty interior. */
    fun build(level: ServerLevel, plot: Plot) {
        val wall = ModBlocks.PLOT_WALL.get().defaultBlockState()
        val air = Blocks.AIR.defaultBlockState()
        val size = plot.outerSize
        val origin = plot.origin
        for (x in 0 until size) for (y in 0 until size) for (z in 0 until size) {
            val edge = x == 0 || y == 0 || z == 0 || x == size - 1 || y == size - 1 || z == size - 1
            level.setBlock(BlockPos(origin.x + x, origin.y + y, origin.z + z), if (edge) wall else air, 2)
        }
        level.setBlock(plot.exitPos, ModBlocks.BACKPACK_EXIT.get().defaultBlockState(), 2)
    }
}
