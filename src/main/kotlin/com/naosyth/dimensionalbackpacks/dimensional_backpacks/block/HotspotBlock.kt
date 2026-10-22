package com.naosyth.dimensionalbackpacks.dimensional_backpacks.block

import com.mojang.serialization.MapCodec
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.item.ModItems
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.PlotManager
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.BlockHitResult

/**
 * A wall block of a plot carrying a hotspot decal on one face. It takes no room volume: it replaces
 * a wall block (see HotspotItem). FACING points from the wall into the room, so the block touching
 * the hotspot is the one at pos.relative(FACING).
 */
class HotspotBlock(properties: Properties) : Block(properties) {
    init {
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH))
    }

    override fun codec(): MapCodec<out Block> = CODEC

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING)
    }

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, movedByPiston: Boolean) {
        super.onPlace(state, level, pos, oldState, movedByPiston)
        if (level !is ServerLevel || oldState.`is`(this)) return
        val manager = PlotManager.get(level)
        val plot = manager.plotAt(pos) ?: return
        manager.addHotspot(plot, pos)
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, movedByPiston: Boolean) {
        if (level is ServerLevel && !newState.`is`(this)) {
            val manager = PlotManager.get(level)
            manager.plotAt(pos)?.let { manager.removeHotspot(it, pos) }
            // Whatever removed the hotspot (e.g. creative mode), the wall must never be left with a hole.
            if (newState.isAir) level.setBlock(pos, ModBlocks.PLOT_WALL.get().defaultBlockState(), 3)
        }
        super.onRemove(state, level, pos, newState, movedByPiston)
    }

    override fun useWithoutItem(state: BlockState, level: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        if (!player.mainHandItem.isEmpty) return InteractionResult.PASS
        if (level is ServerLevel) {
            if (player.isShiftKeyDown) {
                // Sneak with an empty hand takes the hotspot back out of the wall.
                level.setBlock(pos, ModBlocks.PLOT_WALL.get().defaultBlockState(), 3)
                val item = ItemStack(ModItems.HOTSPOT.get())
                if (!player.inventory.add(item)) {
                    level.addFreshEntity(ItemEntity(level, player.x, player.y, player.z, item))
                }
            } else {
                // Otherwise an empty hand marks this hotspot as the primary one.
                val manager = PlotManager.get(level)
                manager.plotAt(pos)?.let { manager.setPrimary(it, pos) }
                player.displayClientMessage(Component.literal("Primary hotspot set"), true)
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide)
    }

    companion object {
        val CODEC: MapCodec<HotspotBlock> = simpleCodec(::HotspotBlock)
        val FACING = BlockStateProperties.FACING
    }
}
