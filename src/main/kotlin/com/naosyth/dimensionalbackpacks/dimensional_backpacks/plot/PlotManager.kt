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
    fun plotAt(pos: BlockPos): Plot? = idAt(pos)?.let { plots[it] }?.takeIf { it.contains(pos) }

    /** Updates the interior tint, telling everyone currently in the pocket dimension if it changed. */
    fun setColor(plot: Plot, color: Int?, pocket: ServerLevel) {
        if (plot.color == color) return
        plot.color = color
        setDirty()
        for (player in pocket.players()) PlotColorSync.send(player, plot)
    }

    fun setSpawn(plot: Plot, spawn: Spawn) {
        plot.customSpawn = spawn
        setDirty()
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
            plot.color?.let { entry.putInt("color", it) }
            plot.customSpawn?.let {
                val spawn = CompoundTag()
                spawn.putDouble("x", it.x)
                spawn.putDouble("y", it.y)
                spawn.putDouble("z", it.z)
                spawn.putFloat("yaw", it.yaw)
                spawn.putFloat("pitch", it.pitch)
                entry.put("spawnPoint", spawn)
            }
            val groups = ListTag()
            for (group in plot.groups) {
                val storages = ListTag()
                for (pos in group.order) {
                    val tag = CompoundTag()
                    tag.putLong("pos", pos.asLong())
                    tag.putBoolean("enabled", pos in group.enabled)
                    storages.add(tag)
                }
                val groupTag = CompoundTag()
                groupTag.put("storages", storages)
                groupTag.putString("name", group.name)
                groups.add(groupTag)
            }
            entry.put("groups", groups)
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
                if (entry.contains("color")) plot.color = entry.getInt("color")
                if (entry.contains("spawnPoint")) {
                    val s = entry.getCompound("spawnPoint")
                    plot.customSpawn = Spawn(s.getDouble("x"), s.getDouble("y"), s.getDouble("z"), s.getFloat("yaw"), s.getFloat("pitch"))
                }
                val groups = entry.getList("groups", Tag.TAG_COMPOUND.toInt())
                for (g in 0 until groups.size) {
                    val group = StorageGroup(name = groups.getCompound(g).getString("name"))
                    val storages = groups.getCompound(g).getList("storages", Tag.TAG_COMPOUND.toInt())
                    for (j in 0 until storages.size) {
                        val storage = storages.getCompound(j)
                        val pos = BlockPos.of(storage.getLong("pos"))
                        group.order.add(pos)
                        if (storage.getBoolean("enabled")) group.enabled.add(pos)
                    }
                    plot.groups.add(group)
                }
                manager.plots[plot.id] = plot
            }
            return manager
        }

        /** Id of the plot whose grid cell contains [pos]; the cell index equals id - 1. Safe to call on the client. */
        fun idAt(pos: BlockPos): Int? {
            if (pos.x < 0 || pos.z < 0) return null
            val cellX = pos.x / CELL_SIZE
            if (cellX >= CELLS_PER_ROW) return null
            return (pos.z / CELL_SIZE) * CELLS_PER_ROW + cellX + 1
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
