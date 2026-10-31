package com.naosyth.dimensionalbackpacks.dimensional_backpacks.block

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.item.ModComponents
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.PlotManager
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.PlotTravel
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.Tier
import net.minecraft.core.BlockPos
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.storage.StorageMenu
import net.minecraft.core.GlobalPos
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.component.DyedItemColor
import net.minecraft.core.Direction
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.DirectionProperty
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.stats.Stats
import net.minecraft.world.InteractionHand
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.item.DyeItem
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import com.google.gson.JsonParser
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.DimensionalBackpacks
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
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

    init {
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? =
        defaultBlockState().setValue(FACING, context.horizontalDirection.opposite)

    override fun rotate(state: BlockState, rotation: Rotation): BlockState =
        state.setValue(FACING, rotation.rotate(state.getValue(FACING)))

    override fun mirror(state: BlockState, mirror: Mirror): BlockState =
        state.rotate(mirror.getRotation(state.getValue(FACING)))

    override fun getShape(
        state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext
    ): VoxelShape = shapes.getValue(state.getValue(FACING))

    private val shapes = shapesFor(tier)

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
        be.color = stack.get(DataComponents.DYED_COLOR)?.rgb
        if (level !is ServerLevel) {
            // Client prediction: show the dye immediately instead of waiting for the server's update.
            level.sendBlockUpdated(pos, state, state, 3)
            return
        }
        be.plotId = stack.get(ModComponents.PLOT_ID.get()) ?: PlotManager.allocate(level.server, tier)?.id ?: 0
        level.sendBlockUpdated(pos, state, state, 3)
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
        PlotManager.get(pocket).setColor(plot, be.color, pocket)
        if (!player.isShiftKeyDown) {
            StorageMenu.open(player, plot, GlobalPos.of(level.dimension(), pos))
            return InteractionResult.CONSUME
        }
        PlotTravel.enter(player, plot, pocket)
        return InteractionResult.CONSUME
    }

    /** Dyes the placed backpack, mixing with its current colour like armor. Other items fall through to the normal click. */
    override fun useItemOn(
        stack: ItemStack, state: BlockState, level: Level, pos: BlockPos, player: Player, hand: InteractionHand, hit: BlockHitResult
    ): ItemInteractionResult {
        val dye = stack.item as? DyeItem ?: return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
        val be = level.getBlockEntity(pos) as? BackpackBlockEntity ?: return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
        if (level.isClientSide) return ItemInteractionResult.SUCCESS

        val current = ItemStack(this)
        be.color?.let { current.set(DataComponents.DYED_COLOR, DyedItemColor(it, true)) }
        be.color = DyedItemColor.applyDyes(current, listOf(dye)).get(DataComponents.DYED_COLOR)?.rgb
        be.setChanged()
        level.sendBlockUpdated(pos, state, state, 3)
        if (level is ServerLevel) {
            PlotManager.getPocketLevel(level.server)?.let { pocket ->
                val manager = PlotManager.get(pocket)
                manager.get(be.plotId)?.let { manager.setColor(it, be.color, pocket) }
            }
        }
        if (!player.abilities.instabuild) stack.shrink(1)
        return ItemInteractionResult.CONSUME
    }

    /** Breaking gives the backpack straight to the player; it only drops in the world if their inventory is full. */
    override fun playerDestroy(
        level: Level, player: Player, pos: BlockPos, state: BlockState, blockEntity: BlockEntity?, tool: ItemStack
    ) {
        val be = blockEntity as? BackpackBlockEntity
        if (be == null || level.isClientSide) return super.playerDestroy(level, player, pos, state, blockEntity, tool)
        val stack = ItemStack(this)
        if (be.plotId != 0) stack.set(ModComponents.PLOT_ID.get(), be.plotId)
        be.color?.let { stack.set(DataComponents.DYED_COLOR, DyedItemColor(it, true)) }
        player.awardStat(Stats.BLOCK_MINED.get(this))
        player.causeFoodExhaustion(0.005f)
        if (!player.inventory.add(stack)) popResource(level, pos, stack)
    }

    override fun getDrops(state: BlockState, params: LootParams.Builder): MutableList<ItemStack> {
        val stack = ItemStack(this)
        val be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) as? BackpackBlockEntity
        if (be != null && be.plotId != 0) stack.set(ModComponents.PLOT_ID.get(), be.plotId)
        be?.color?.let { stack.set(DataComponents.DYED_COLOR, DyedItemColor(it, true)) }
        return mutableListOf(stack)
    }

    companion object {
        val FACING: DirectionProperty = BlockStateProperties.HORIZONTAL_FACING

        private val FULL_CUBE = listOf(doubleArrayOf(0.0, 0.0, 0.0, 16.0, 16.0, 16.0))

        /** Reads each element's from/to out of the tier's block model, so shapes follow Blockbench exports. */
        private fun loadBoxes(tier: Tier): List<DoubleArray> {
            val path = "/assets/${DimensionalBackpacks.ID}/models/block/backpack_tier_${tier.ordinal + 1}.json"
            return try {
                val stream = BackpackBlock::class.java.getResourceAsStream(path) ?: return FULL_CUBE
                val root = stream.bufferedReader().use { JsonParser.parseReader(it).asJsonObject }
                root.getAsJsonArray("elements").map { element ->
                    val obj = element.asJsonObject
                    val from = obj.getAsJsonArray("from").map { it.asDouble }
                    val to = obj.getAsJsonArray("to").map { it.asDouble }
                    DoubleArray(6) { if (it < 3) from[it] else to[it - 3] }
                }.ifEmpty { FULL_CUBE }
            } catch (e: Exception) {
                DimensionalBackpacks.LOGGER.warn("Could not read the shape of $path, using a full cube", e)
                FULL_CUBE
            }
        }

        // Rotates the north-facing boxes clockwise (seen from above) to match the blockstate's y rotation.
        private fun shapesFor(tier: Tier): Map<Direction, VoxelShape> {
            val boxes = loadBoxes(tier)
            fun build(map: (DoubleArray) -> VoxelShape) =
                boxes.map(map).reduce { a, b -> Shapes.or(a, b) }.optimize()
            return mapOf(
                Direction.NORTH to build { b -> box(b[0], b[1], b[2], b[3], b[4], b[5]) },
                Direction.EAST to build { b -> box(16 - b[5], b[1], b[0], 16 - b[2], b[4], b[3]) },
                Direction.SOUTH to build { b -> box(16 - b[3], b[1], 16 - b[5], 16 - b[0], b[4], 16 - b[2]) },
                Direction.WEST to build { b -> box(b[2], b[1], 16 - b[3], b[5], b[4], 16 - b[0]) }
            )
        }

        val CODEC: MapCodec<BackpackBlock> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                Codec.INT.fieldOf("tier").forGetter { it.tier.ordinal },
                propertiesCodec()
            ).apply(instance) { tier, props -> BackpackBlock(Tier.fromOrdinal(tier), props) }
        }
    }
}
