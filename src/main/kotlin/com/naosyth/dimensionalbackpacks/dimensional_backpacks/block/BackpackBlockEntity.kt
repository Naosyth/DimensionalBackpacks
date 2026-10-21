package com.naosyth.dimensionalbackpacks.dimensional_backpacks.block

import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.PlotLoader
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.PlotManager
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

class BackpackBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(ModBlockEntities.BACKPACK.get(), pos, state) {

    /** 0 means no plot has been assigned yet. */
    var plotId: Int = 0

    /** Called every tick on the server while this chunk is ticking; keeps the interior loaded. */
    fun serverTick(level: Level) {
        if (level.gameTime % PlotLoader.REFRESH_INTERVAL != 0L || level !is ServerLevel) return
        val pocket = PlotManager.getPocketLevel(level.server) ?: return
        val plot = PlotManager.get(pocket).get(plotId) ?: return
        PlotLoader.keepLoaded(level.server, plot)
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.putInt("plotId", plotId)
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        plotId = tag.getInt("plotId")
    }
}
