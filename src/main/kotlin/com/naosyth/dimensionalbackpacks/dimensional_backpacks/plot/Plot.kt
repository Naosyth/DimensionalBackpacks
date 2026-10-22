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

    /** Hotspot slot (0-based, matching the keybind) to the position of its hotspot block. */
    val hotspots = HashMap<Int, BlockPos>()

    /** Slot chosen as primary by the player, or -1 for none. */
    var primary = -1

    /** The slot a tap of the hotkey uses: the chosen primary if it still exists, else the lowest slot. */
    fun primarySlot(): Int? = if (primary in hotspots) primary else hotspots.keys.minOrNull()

    /** True for any block of the plot, walls included. */
    fun contains(pos: BlockPos): Boolean {
        val o = origin
        return pos.x in o.x until o.x + outerSize && pos.y in o.y until o.y + outerSize && pos.z in o.z until o.z + outerSize
    }
}
