package com.naosyth.dimensionalbackpacks.dimensional_backpacks.block

import com.mojang.serialization.MapCodec
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.DyedItemColor
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.IntegerProperty
import net.minecraft.world.level.storage.loot.LootParams

/**
 * Breakable look-alike of the plot wall for building inside a backpack. It never carries hotspots.
 * Its dye is a fixed property of the block (0 = undyed, otherwise dye id + 1) and does not follow the backpack's dye.
 */
class DecorativeWallBlock(properties: Properties) : Block(properties) {
    init {
        registerDefaultState(stateDefinition.any().setValue(DYE, 0))
    }

    override fun codec(): MapCodec<out Block> = CODEC

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(DYE)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        val rgb = context.itemInHand.get(DataComponents.DYED_COLOR)?.rgb?.and(0xFFFFFF)
        val index = DyeColor.entries.indexOfFirst { it.textureDiffuseColor and 0xFFFFFF == rgb }
        return defaultBlockState().setValue(DYE, index + 1)
    }

    override fun getDrops(state: BlockState, params: LootParams.Builder): MutableList<ItemStack> =
        mutableListOf(stackFor(state))

    override fun getCloneItemStack(state: BlockState, target: net.minecraft.world.phys.HitResult, level: LevelReader, pos: BlockPos, player: net.minecraft.world.entity.player.Player): ItemStack =
        stackFor(state)

    private fun stackFor(state: BlockState): ItemStack {
        val stack = ItemStack(this)
        val dye = state.getValue(DYE)
        if (dye > 0) stack.set(DataComponents.DYED_COLOR, DyedItemColor(DyeColor.byId(dye - 1).textureDiffuseColor and 0xFFFFFF, false))
        return stack
    }

    companion object {
        val CODEC: MapCodec<DecorativeWallBlock> = simpleCodec(::DecorativeWallBlock)
        val DYE: IntegerProperty = IntegerProperty.create("dye", 0, 16)

        /** RGB tint for a block state's dye, or white when undyed. */
        fun rgb(state: BlockState?): Int {
            val dye = state?.takeIf { it.hasProperty(DYE) }?.getValue(DYE) ?: 0
            return if (dye == 0) 0xFFFFFF else DyeColor.byId(dye - 1).textureDiffuseColor and 0xFFFFFF
        }
    }
}
