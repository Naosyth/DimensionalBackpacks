package com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot

import net.minecraft.core.BlockPos

/** An exact standing position and view direction. */
data class Spawn(val x: Double, val y: Double, val z: Double, val yaw: Float, val pitch: Float) {
    val blockPos: BlockPos get() = BlockPos.containing(x, y, z)
}

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

    /** Default place for a player arriving from outside to stand: the middle of the floor. */
    val defaultSpawnPos: BlockPos
        get() = origin.offset(outerSize / 2, 1, outerSize / 2)

    /** Where arriving players stand and look, as set with the zipper, or null to use the default. */
    var customSpawn: Spawn? = null

    /** True for blocks inside the wall shell. */
    fun isInterior(pos: BlockPos): Boolean {
        val o = origin
        val range = 1 until outerSize - 1
        return pos.x - o.x in range && pos.y - o.y in range && pos.z - o.z in range
    }

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
