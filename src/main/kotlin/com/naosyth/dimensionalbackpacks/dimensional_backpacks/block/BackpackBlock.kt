package com.naosyth.dimensionalbackpacks.dimensional_backpacks.block

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.item.ModComponents
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.PlotManager
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.PlotTravel
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.Tier
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import net.minecraft.world.phys.BlockHitResult

/** A placed backpack. Right-click to enter its interior. */
class BackpackBlock(val tier: Tier, properties: Properties) : Block(properties), EntityBlock {
    override fun codec(): MapCodec<out Block> = CODEC

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = BackpackBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        level: Level, state: BlockState, type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        if (level.isClientSide || type != ModBlockEntities.BACKPACK.get()) return null
        return BlockEntityTicker { l, _, _, be -> (be as BackpackBlockEntity).serverTick(l) }
    }

    override fun setPlacedBy(level: Level, pos: BlockPos, state: BlockState, placer: LivingEntity?, stack: ItemStack) {
        super.setPlacedBy(level, pos, state, placer, stack)
        val be = level.getBlockEntity(pos) as? BackpackBlockEntity ?: return
        if (level !is ServerLevel) return
        be.plotId = stack.get(ModComponents.PLOT_ID.get()) ?: PlotManager.allocate(level.server, tier)?.id ?: 0
        be.setChanged()
    }

    override fun useWithoutItem(
        state: BlockState, level: Level, pos: BlockPos, player: Player, hit: BlockHitResult
    ): InteractionResult {
        if (level !is ServerLevel || player !is ServerPlayer) return InteractionResult.sidedSuccess(level.isClientSide)
        val be = level.getBlockEntity(pos) as? BackpackBlockEntity ?: return InteractionResult.PASS
        val pocket = PlotManager.getPocketLevel(level.server)
        if (pocket == null) {
            player.displayClientMessage(Component.literal("The pocket dimension is not available"), true)
            return InteractionResult.FAIL
        }
        var plot = PlotManager.get(pocket).get(be.plotId)
        if (plot == null) {
            plot = PlotManager.allocate(level.server, tier) ?: return InteractionResult.FAIL
            be.plotId = plot.id
            be.setChanged()
        }
        PlotTravel.enter(player, plot, pocket)
        return InteractionResult.CONSUME
    }

    override fun getDrops(state: BlockState, params: LootParams.Builder): MutableList<ItemStack> {
        val stack = ItemStack(this)
        val be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) as? BackpackBlockEntity
        if (be != null && be.plotId != 0) stack.set(ModComponents.PLOT_ID.get(), be.plotId)
        return mutableListOf(stack)
    }

    companion object {
        val CODEC: MapCodec<BackpackBlock> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                Codec.INT.fieldOf("tier").forGetter { it.tier.ordinal },
                propertiesCodec()
            ).apply(instance) { tier, props -> BackpackBlock(Tier.fromOrdinal(tier), props) }
        }
    }
}
