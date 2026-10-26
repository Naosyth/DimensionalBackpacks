package com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot

import com.naosyth.dimensionalbackpacks.dimensional_backpacks.DimensionalBackpacks
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.config.ModConfig
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.Tag

/** A tier's interior, read from its structure file. Coordinates are structure-local (0 until size). */
class PlotLayout(
    val sizeX: Int,
    val sizeY: Int,
    val sizeZ: Int,
    private val walls: BooleanArray,
    private val interior: BooleanArray,
    /** Where players arrive by default, marked in the structure by a gold block. */
    val spawn: Triple<Int, Int, Int>
) {
    private fun index(x: Int, y: Int, z: Int) = (y * sizeZ + z) * sizeX + x
    private fun inBounds(x: Int, y: Int, z: Int) = x in 0 until sizeX && y in 0 until sizeY && z in 0 until sizeZ

    fun isWall(x: Int, y: Int, z: Int) = inBounds(x, y, z) && walls[index(x, y, z)]
    fun isInterior(x: Int, y: Int, z: Int) = inBounds(x, y, z) && interior[index(x, y, z)]

    companion object {
        private const val WALL = "${DimensionalBackpacks.ID}:plot_wall"
        private const val MARKER = "minecraft:gold_block"

        fun load(tier: Tier): PlotLayout {
            val path = "/data/${DimensionalBackpacks.ID}/structure/backpack_tier_${tier.ordinal + 1}.nbt"
            val stream = PlotLayout::class.java.getResourceAsStream(path)
                ?: error("Missing structure file $path")
            val root = stream.use { NbtIo.readCompressed(it, NbtAccounter.unlimitedHeap()) }
            val size = root.getList("size", Tag.TAG_INT.toInt())
            val sx = size.getInt(0)
            val sy = size.getInt(1)
            val sz = size.getInt(2)
            val palette = root.getList("palette", Tag.TAG_COMPOUND.toInt()).map { (it as CompoundTag).getString("Name") }
            val walls = BooleanArray(sx * sy * sz)
            val interior = BooleanArray(sx * sy * sz)
            var marker: Triple<Int, Int, Int>? = null
            for (tag in root.getList("blocks", Tag.TAG_COMPOUND.toInt())) {
                val block = tag as CompoundTag
                val pos = block.getList("pos", Tag.TAG_INT.toInt())
                val x = pos.getInt(0)
                val y = pos.getInt(1)
                val z = pos.getInt(2)
                when (palette[block.getInt("state")]) {
                    WALL -> walls[(y * sz + z) * sx + x] = true
                    MARKER -> marker = Triple(x, y, z)
                }
            }
            val start = marker ?: error("Structure $path has no gold block spawn marker")

            // Flood-fill from the marker: the interior is everything reachable without crossing a wall.
            val stack = ArrayDeque<Triple<Int, Int, Int>>()
            stack.add(start)
            while (stack.isNotEmpty()) {
                val (x, y, z) = stack.removeLast()
                if (x !in 0 until sx || y !in 0 until sy || z !in 0 until sz) {
                    error("Structure $path is not sealed by walls")
                }
                val i = (y * sz + z) * sx + x
                if (walls[i] || interior[i]) continue
                interior[i] = true
                stack.add(Triple(x + 1, y, z)); stack.add(Triple(x - 1, y, z))
                stack.add(Triple(x, y + 1, z)); stack.add(Triple(x, y - 1, z))
                stack.add(Triple(x, y, z + 1)); stack.add(Triple(x, y, z - 1))
            }
            return PlotLayout(sx, sy, sz, walls, interior, start)
        }
    }
}

/** Backpack tiers. Interior shapes live in `data/<mod>/structure/backpack_tier_N.nbt`. */
enum class Tier {
    TIER_1, TIER_2, TIER_3;

    val maxHotspots: Int get() = ModConfig.maxHotspots(this)

    /** Registry name of this tier's backpack block and item. */
    val registryName: String get() = "backpack_tier_${ordinal + 1}"
    val layout: PlotLayout by lazy { PlotLayout.load(this) }

    companion object {
        fun fromOrdinal(ordinal: Int): Tier = entries.getOrElse(ordinal) { TIER_1 }
    }
}
