package com.naosyth.dimensionalbackpacks.dimensional_backpacks.block

import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
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

    /** RGB dye of the backpack, or null for undyed. Placed backpacks keep it so dropping them preserves it. */
    var color: Int? = null
        set(value) {
            field = value
            setChanged()
        }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag = saveWithoutMetadata(registries)

    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket = ClientboundBlockEntityDataPacket.create(this)

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.putInt("plotId", plotId)
        color?.let { tag.putInt("color", it) }
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        plotId = tag.getInt("plotId")
        val loaded = if (tag.contains("color")) tag.getInt("color") else null
        val changed = loaded != color
        color = loaded
        // The tint is baked into the chunk mesh, so a client that learns the colour late must remesh.
        if (changed && level?.isClientSide == true) level?.sendBlockUpdated(blockPos, blockState, blockState, 3)
    }
}
