package com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot

import com.naosyth.dimensionalbackpacks.dimensional_backpacks.block.ModBlocks
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.Blocks

object PlotBuilder {
    /** Builds the walls and clears the interior, as laid out by the tier's structure file. */
    fun build(level: ServerLevel, plot: Plot) {
        val wall = ModBlocks.PLOT_WALL.get().defaultBlockState()
        val air = Blocks.AIR.defaultBlockState()
        val l = plot.tier.layout
        val origin = plot.origin
        for (x in 0 until l.sizeX) for (y in 0 until l.sizeY) for (z in 0 until l.sizeZ) {
            val state = when {
                l.isInterior(x, y, z) -> air
                l.isWall(x, y, z) -> wall
                else -> continue
            }
            level.setBlock(BlockPos(origin.x + x, origin.y + y, origin.z + z), state, 2)
        }
    }
}
