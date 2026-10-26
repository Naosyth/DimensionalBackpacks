package com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction

/** An exact standing position and view direction. */
data class Spawn(val x: Double, val y: Double, val z: Double, val yaw: Float, val pitch: Float) {
    val blockPos: BlockPos get() = BlockPos.containing(x, y, z)
}

/** A hotspot decal on [face] of the wall block at [pos]. The face points into the room. */
data class Hotspot(val pos: BlockPos, val face: Direction)

/** A backpack interior. Plots are permanent and never store an owner. */
data class Plot(val id: Int, val cell: Int, var tier: Tier) {
    /** Chunk-aligned corner of the plot's bounding box. */
    val origin: BlockPos
        get() = BlockPos(
            (cell % PlotManager.CELLS_PER_ROW) * PlotManager.CELL_SIZE,
            PlotManager.PLOT_Y,
            (cell / PlotManager.CELLS_PER_ROW) * PlotManager.CELL_SIZE
        )

    /** World position of the structure's local (0, 0, 0). */
    val interiorOrigin: BlockPos get() = origin

    /** Default place for a player arriving from outside to stand: the structure's gold marker block. */
    val defaultSpawnPos: BlockPos
        get() = tier.layout.spawn.let { origin.offset(it.first, it.second, it.third) }

    /** Where arriving players stand and look, as set with the zipper, or null to use the default. */
    var customSpawn: Spawn? = null

    /** RGB tint of the interior walls, taken from the backpack's dye, or null for undyed. */
    var color: Int? = null

    /** True for open blocks inside the wall shell, nooks included. */
    fun isInterior(pos: BlockPos): Boolean {
        val local = pos.subtract(origin)
        return tier.layout.isInterior(local.x, local.y, local.z)
    }

    /** Hotspot slot (0-based, matching the keybind) to the position of its hotspot block. */
    val hotspots = HashMap<Int, Hotspot>()

    /** Slot chosen as primary by the player, or -1 for none. */
    var primary = -1

    /** The slot a tap of the hotkey uses: the chosen primary if it still exists, else the lowest slot. */
    fun primarySlot(): Int? {
        val active = hotspots.keys.filter { it < tier.maxHotspots }
        return if (primary in active) primary else active.minOrNull()
    }

    /** True for any block of the plot's bounding box, walls included. */
    fun contains(pos: BlockPos): Boolean {
        val o = origin
        val l = tier.layout
        return pos.x in o.x until o.x + l.sizeX && pos.y in o.y until o.y + l.sizeY && pos.z in o.z until o.z + l.sizeZ
    }
}
