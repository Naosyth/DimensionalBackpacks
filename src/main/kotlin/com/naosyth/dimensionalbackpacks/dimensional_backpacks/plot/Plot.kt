package com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot

import net.minecraft.core.BlockPos

/** A backpack interior. Plots are permanent and never store an owner. */
data class Plot(val id: Int, val cell: Int, var tier: Tier) {
    val origin: BlockPos
        get() = BlockPos(
            (cell % PlotManager.CELLS_PER_ROW) * PlotManager.CELL_SIZE,
            PlotManager.PLOT_Y,
            (cell / PlotManager.CELLS_PER_ROW) * PlotManager.CELL_SIZE
        )

    /** Outer size including the 1-block wall shell. */
    val outerSize: Int get() = tier.interiorSize + 2

    /** The exit block sits in the middle of one wall, at standing height. */
    val exitPos: BlockPos get() = origin.offset(outerSize / 2, 1, 0)

    /** Where a player arriving from outside should stand. */
    val spawnPos: BlockPos
        get() = origin.offset(outerSize / 2, 1, outerSize / 2)
}
