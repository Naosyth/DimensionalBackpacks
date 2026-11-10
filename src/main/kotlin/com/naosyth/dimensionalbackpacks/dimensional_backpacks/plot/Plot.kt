package com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot

import net.minecraft.core.BlockPos
import net.minecraft.core.NonNullList
import net.minecraft.world.item.ItemStack

/** An exact standing position and view direction. */
data class Spawn(val x: Double, val y: Double, val z: Double, val yaw: Float, val pitch: Float) {
    val blockPos: BlockPos get() = BlockPos.containing(x, y, z)
}

/**
 * One aggregated view of the plot's storages. [order] lists every storage in the plot by priority, top
 * first; only those in [enabled] belong to the group. A blank [name] means the group is shown by its number.
 */
class StorageGroup(
    val order: ArrayList<BlockPos> = ArrayList(),
    val enabled: HashSet<BlockPos> = HashSet(),
    var name: String = "",
    var fillPartialStacksFirst: Boolean = true
) {
    companion object {
        const val MAX = 9
        const val MAX_NAME = 24
    }
}

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

    /** User-defined storage groups, in display order. Maintained by PlotStorage, which keeps at least one. */
    val groups = ArrayList<StorageGroup>()

    val craftingItems: NonNullList<ItemStack> = NonNullList.withSize(9, ItemStack.EMPTY)

    /** True for any block of the plot's bounding box, walls included. */
    fun contains(pos: BlockPos): Boolean {
        val o = origin
        val l = tier.layout
        return pos.x in o.x until o.x + l.sizeX && pos.y in o.y until o.y + l.sizeY && pos.z in o.z until o.z + l.sizeZ
    }
}
