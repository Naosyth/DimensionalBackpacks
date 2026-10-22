package com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot

import com.naosyth.dimensionalbackpacks.dimensional_backpacks.DimensionalBackpacks
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.saveddata.SavedData

/** Registry of all plots, stored with the pocket dimension's level data. */
class PlotManager : SavedData() {
    private val plots = HashMap<Int, Plot>()
    private var nextId = 1

    fun get(id: Int): Plot? = plots[id]

    /** Finds the plot containing [pos], or null if it is outside every plot. */
    fun plotAt(pos: BlockPos): Plot? {
        if (pos.x < 0 || pos.z < 0) return null
        val cellX = pos.x / CELL_SIZE
        if (cellX >= CELLS_PER_ROW) return null
        val cell = (pos.z / CELL_SIZE) * CELLS_PER_ROW + cellX
        // Cell index equals id - 1.
        return plots[cell + 1]?.takeIf { it.contains(pos) }
    }

    fun hasFreeHotspotSlot(plot: Plot): Boolean = (0 until plot.tier.maxHotspots).any { it !in plot.hotspots }

    /** Registers a hotspot in the lowest free slot, or returns null if the tier's limit is reached. */
    fun addHotspot(plot: Plot, pos: BlockPos): Int? {
        val slot = (0 until plot.tier.maxHotspots).firstOrNull { it !in plot.hotspots } ?: return null
        plot.hotspots[slot] = pos.immutable()
        setDirty()
        return slot
    }

    fun setPrimary(plot: Plot, pos: BlockPos) {
        val slot = plot.hotspots.entries.firstOrNull { it.value == pos }?.key ?: return
        plot.primary = slot
        setDirty()
    }

    fun removeHotspot(plot: Plot, pos: BlockPos) {
        if (plot.hotspots.values.removeIf { it == pos }) setDirty()
    }

    /** Allocates a new plot in the next free grid cell. Cell index equals id - 1, so cells are never reused. */
    fun create(tier: Tier): Plot {
        val id = nextId++
        val plot = Plot(id, id - 1, tier)
        plots[id] = plot
        setDirty()
        return plot
    }

    override fun save(tag: CompoundTag, registries: HolderLookup.Provider): CompoundTag {
        val list = ListTag()
        for (plot in plots.values) {
            val entry = CompoundTag()
            entry.putInt("id", plot.id)
            entry.putInt("cell", plot.cell)
            entry.putInt("tier", plot.tier.ordinal)
            entry.putInt("primary", plot.primary)
            val hotspots = ListTag()
            for ((slot, pos) in plot.hotspots) {
                val hotspot = CompoundTag()
                hotspot.putInt("slot", slot)
                hotspot.putLong("pos", pos.asLong())
                hotspots.add(hotspot)
            }
            entry.put("hotspots", hotspots)
            list.add(entry)
        }
        tag.put("plots", list)
        tag.putInt("nextId", nextId)
        return tag
    }

    companion object {
        /** 16 chunks per cell side. */
        const val CELL_SIZE = 256
        const val CELLS_PER_ROW = 1024
        const val PLOT_Y = 64

        val DIMENSION: ResourceKey<Level> = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(DimensionalBackpacks.ID, "pocket")
        )

        private fun load(tag: CompoundTag, registries: HolderLookup.Provider): PlotManager {
            val manager = PlotManager()
            manager.nextId = tag.getInt("nextId")
            val list = tag.getList("plots", Tag.TAG_COMPOUND.toInt())
            for (i in 0 until list.size) {
                val entry = list.getCompound(i)
                val plot = Plot(entry.getInt("id"), entry.getInt("cell"), Tier.fromOrdinal(entry.getInt("tier")))
                plot.primary = if (entry.contains("primary")) entry.getInt("primary") else -1
                val hotspots = entry.getList("hotspots", Tag.TAG_COMPOUND.toInt())
                for (j in 0 until hotspots.size) {
                    val hotspot = hotspots.getCompound(j)
                    plot.hotspots[hotspot.getInt("slot")] = BlockPos.of(hotspot.getLong("pos"))
                }
                manager.plots[plot.id] = plot
            }
            return manager
        }

        fun getPocketLevel(server: MinecraftServer): ServerLevel? = server.getLevel(DIMENSION)

        /** Creates and builds a new plot, or returns null if the pocket dimension isn't available. */
        fun allocate(server: MinecraftServer, tier: Tier): Plot? {
            val pocket = getPocketLevel(server) ?: return null
            val plot = get(pocket).create(tier)
            PlotBuilder.build(pocket, plot)
            return plot
        }

        fun get(pocket: ServerLevel): PlotManager =
            pocket.dataStorage.computeIfAbsent(SavedData.Factory(::PlotManager, ::load, null), "dimensional_backpacks_plots")
    }
}
