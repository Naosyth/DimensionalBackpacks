package com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot

import com.naosyth.dimensionalbackpacks.dimensional_backpacks.DimensionalBackpacks
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
