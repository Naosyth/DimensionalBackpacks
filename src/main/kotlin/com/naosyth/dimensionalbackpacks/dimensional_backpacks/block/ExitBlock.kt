package com.naosyth.dimensionalbackpacks.dimensional_backpacks.block

import com.mojang.serialization.MapCodec
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.PlotTravel
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult

/** Placed inside every plot; right-click to leave the backpack. */
class ExitBlock(properties: Properties) : Block(properties) {
    override fun codec(): MapCodec<out Block> = CODEC

    override fun useWithoutItem(
        state: BlockState, level: Level, pos: BlockPos, player: Player, hit: BlockHitResult
    ): InteractionResult {
        if (!level.isClientSide && player is ServerPlayer) PlotTravel.exit(player)
        return InteractionResult.sidedSuccess(level.isClientSide)
    }

    companion object {
        val CODEC: MapCodec<ExitBlock> = simpleCodec(::ExitBlock)
    }
}
