package com.naosyth.dimensionalbackpacks.dimensional_backpacks.storage

import com.naosyth.dimensionalbackpacks.dimensional_backpacks.config.ModConfig
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.Plot
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.PlotManager
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.StorageGroup
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.ChestBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.ChestType
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.items.IItemHandler
import net.neoforged.neoforge.items.ItemHandlerHelper

/** Finds the item storages inside a plot and reads, inserts into and extracts from them as one pool. */
object PlotStorage {
    class Storage(val pos: BlockPos, val state: BlockState, val handler: IItemHandler)

    class Total(val stack: ItemStack, val count: Long)

    private class Key(val stack: ItemStack) {
        override fun equals(other: Any?) = other is Key && ItemStack.isSameItemSameComponents(stack, other.stack)
        override fun hashCode() = ItemStack.hashItemAndComponents(stack)
    }

    private fun handlerAt(level: ServerLevel, pos: BlockPos, state: BlockState): IItemHandler? {
        // A double chest exposes both halves through either block; only count it once.
        if (state.hasProperty(ChestBlock.TYPE) && state.getValue(ChestBlock.TYPE) == ChestType.RIGHT) return null
        level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null)?.let { return it }
        return Direction.entries.firstNotNullOfOrNull { level.getCapability(Capabilities.ItemHandler.BLOCK, pos, it) }
    }

    /**
     * Scans the plot for blocks with an item handler and reconciles the plot's groups with what
     * exists. The plot must be loaded.
     */
    fun detect(pocket: ServerLevel, plot: Plot): Map<BlockPos, Storage> {
        val found = LinkedHashMap<BlockPos, Pair<BlockState, IItemHandler>>()
        val origin = plot.origin
        val layout = plot.tier.layout
        for (cx in (origin.x shr 4)..((origin.x + layout.sizeX - 1) shr 4)) {
            for (cz in (origin.z shr 4)..((origin.z + layout.sizeZ - 1) shr 4)) {
                if (!pocket.hasChunk(cx, cz)) continue
                for (pos in pocket.getChunk(cx, cz).blockEntitiesPos.sortedBy { it.asLong() }) {
                    if (!plot.contains(pos) || !plot.isInterior(pos)) continue
                    val state = pocket.getBlockState(pos)
                    val handler = handlerAt(pocket, pos, state) ?: continue
                    found[pos.immutable()] = state to handler
                }
            }
        }
        if (reconcileGroups(plot, found.keys)) PlotManager.get(pocket).setDirty()
        return found.mapValues { (pos, stateAndHandler) -> Storage(pos, stateAndHandler.first, stateAndHandler.second) }
    }

    /** The enabled storages of [group], in priority order. */
    fun members(group: StorageGroup, all: Map<BlockPos, Storage>): List<Storage> =
        group.order.filter { it in group.enabled }.mapNotNull { all[it] }

    /** Drops vanished storages from every group and adds new ones at the bottom, enabled only in the first group. */
    private fun reconcileGroups(plot: Plot, existing: Set<BlockPos>): Boolean {
        var changed = plot.groups.isEmpty()
        if (changed) plot.groups.add(StorageGroup())
        plot.groups.forEachIndexed { index, group ->
            changed = group.order.removeIf { it !in existing } or changed
            changed = group.enabled.retainAll(existing) or changed
            for (pos in existing) {
                if (pos in group.order) continue
                group.order.add(pos)
                if (index == 0 && group.enabled.size < ModConfig.maxStoragesPerGroup(plot.tier)) group.enabled.add(pos)
                changed = true
            }
        }
        return changed
    }

    /** One entry per item type across the given storages, in a stable order. */
    fun totals(storages: List<Storage>): List<Total> {
        val counts = LinkedHashMap<Key, Long>()
        for (storage in storages) {
            for (slot in 0 until storage.handler.slots) {
                val stack = storage.handler.getStackInSlot(slot)
                if (stack.isEmpty) continue
                counts.merge(Key(stack.copyWithCount(1)), stack.count.toLong(), Long::plus)
            }
        }
        return counts.map { (key, count) -> Total(key.stack, count) }
            .sortedWith(compareBy({ BuiltInRegistries.ITEM.getKey(it.stack.item).toString() }, { ItemStack.hashItemAndComponents(it.stack) }))
    }

    fun count(storages: List<Storage>, template: ItemStack): Long {
        var total = 0L
        for (storage in storages) {
            for (slot in 0 until storage.handler.slots) {
                val stack = storage.handler.getStackInSlot(slot)
                if (ItemStack.isSameItemSameComponents(stack, template)) total += stack.count
            }
        }
        return total
    }

    /** Takes up to [amount] (at most one stack) matching [template], from the top storage down. */
    fun extract(storages: List<Storage>, template: ItemStack, amount: Int): ItemStack {
        var remaining = minOf(amount, template.maxStackSize)
        var result = ItemStack.EMPTY
        for (storage in storages) {
            for (slot in 0 until storage.handler.slots) {
                if (remaining <= 0) return result
                if (!ItemStack.isSameItemSameComponents(storage.handler.getStackInSlot(slot), template)) continue
                val taken = storage.handler.extractItem(slot, remaining, false)
                if (taken.isEmpty) continue
                if (result.isEmpty) result = taken else result.grow(taken.count)
                remaining -= taken.count
            }
        }
        return result
    }

    /**
     * Inserts [stack] into [storages], and returns what didn't fit. When [fillPartialStacksFirst] is enabled,
     * matching partial stacks across the whole group are filled before trying other slots in priority order.
     */
    fun insert(storages: List<Storage>, stack: ItemStack, fillPartialStacksFirst: Boolean = false): ItemStack {
        var remaining = stack
        if (fillPartialStacksFirst) {
            for (storage in storages) {
                for (slot in 0 until storage.handler.slots) {
                    if (remaining.isEmpty) return ItemStack.EMPTY
                    val current = storage.handler.getStackInSlot(slot)
                    if (current.isEmpty || !ItemStack.isSameItemSameComponents(current, remaining)) continue
                    if (current.count >= minOf(current.maxStackSize, storage.handler.getSlotLimit(slot))) continue
                    remaining = storage.handler.insertItem(slot, remaining, false)
                }
            }
        }
        for (storage in storages) {
            if (remaining.isEmpty) continue
            remaining = ItemHandlerHelper.insertItem(storage.handler, remaining, false)
        }
        return remaining
    }
}
