package com.naosyth.dimensionalbackpacks.dimensional_backpacks.api

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.bus.api.Event
import net.neoforged.bus.api.ICancellableEvent

/**
 * Posted on the NeoForge event bus before a hotspot right-clicks its target block. Cancel it to stop
 * the use; [HotspotApi.isUsingHotspot] is already true for [pos] while listeners run.
 */
class HotspotUseEvent(
    val player: ServerPlayer,
    /** The pocket dimension level the block is in. */
    val level: ServerLevel,
    val pos: BlockPos,
    val state: BlockState,
) : Event(), ICancellableEvent
