package com.naosyth.dimensionalbackpacks.dimensional_backpacks.storage

import com.naosyth.dimensionalbackpacks.dimensional_backpacks.config.ModConfig
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.item.BackpackItem
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.item.ModComponents
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.item.WornBackpack
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.network.StorageConfigPayload
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.network.StorageGroupPayload
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.network.StorageSyncPayload
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.Plot
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.PlotLoader
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.PlotManager
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.PlotTravel
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.StorageGroup
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.Tier
import net.minecraft.core.GlobalPos
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Container
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.inventory.ResultContainer
import net.minecraft.world.inventory.Slot
import net.minecraft.world.inventory.TransientCraftingContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.RecipeType
import net.neoforged.neoforge.common.CommonHooks
import net.neoforged.neoforge.event.EventHooks
import net.neoforged.neoforge.network.PacketDistributor

/**
 * The aggregated storage screen of a plot. The player's inventory is made of normal slots; the
 * aggregated item grid is virtual, driven by custom click packets, and synced with [StorageSyncPayload].
 * It shows one of the plot's storage groups at a time, and crafting draws from that same group.
 *
 * The crafting grid is ghost-only: its slots hold one-item templates that are never real items.
 * Clicking one with a stack on the cursor copies the item in without using it up, and clicking it
 * empty clears it. Taking the result consumes the real ingredients from storage (then the player's
 * inventory), and the result only shows while every ingredient is available.
 *
 * Slot indices: 0 result, 1-9 crafting grid, 10-36 inventory, 37-45 hotbar. Slot positions are
 * laid out by the client screen.
 */
class StorageMenu(containerId: Int, private val inventory: Inventory, val plotId: Int, val placed: Boolean = false) :
    AbstractContainerMenu(ModMenus.STORAGE.get(), containerId) {

    /** Set on the server when opened from a placed backpack; the menu closes if the player walks away. */
    var anchor: GlobalPos? = null

    private val craftSlots = TransientCraftingContainer(this, 3, 3)
    private val resultSlots = ResultContainer()
    private var ticks = 0
    private var lastSent: List<Any>? = null
    private var groupIndex = 0

    private class GhostSlot(container: Container, index: Int) : Slot(container, index, 0, 0) {
        override fun mayPlace(stack: ItemStack) = false
        override fun mayPickup(player: Player) = false
    }

    private class ResultOnlySlot(container: Container) : Slot(container, 0, 0, 0) {
        override fun mayPlace(stack: ItemStack) = false
        override fun mayPickup(player: Player) = false
    }

    init {
        addSlot(ResultOnlySlot(resultSlots))
        for (i in 0 until 9) addSlot(GhostSlot(craftSlots, i))
        for (row in 0 until 3) {
            for (col in 0 until 9) addSlot(Slot(inventory, col + row * 9 + 9, 0, 0))
        }
        for (col in 0 until 9) addSlot(Slot(inventory, col, 0, 0))
    }

    // Runs on both sides, so the client predicts ghost changes. The result slot is server-only.
    override fun clicked(slotId: Int, button: Int, clickType: ClickType, player: Player) {
        when (slotId) {
            RESULT_SLOT -> (player as? ServerPlayer)?.let { craftClick(it, clickType) }
            in CRAFT_START until INVENTORY_START -> ghostClick(slotId - CRAFT_START, button, clickType, player)
            else -> super.clicked(slotId, button, clickType, player)
        }
    }

    private fun ghostClick(index: Int, button: Int, clickType: ClickType, player: Player) {
        when (clickType) {
            ClickType.PICKUP -> craftSlots.setItem(index, if (carried.isEmpty) ItemStack.EMPTY else carried.copyWithCount(1))
            ClickType.QUICK_MOVE -> craftSlots.setItem(index, ItemStack.EMPTY)
            ClickType.SWAP -> if (button in 0..8) {
                val stack = player.inventory.getItem(button)
                craftSlots.setItem(index, if (stack.isEmpty) ItemStack.EMPTY else stack.copyWithCount(1))
            }
            else -> {}
        }
    }

    private fun craftClick(player: ServerPlayer, clickType: ClickType) {
        val storages = storages() ?: return
        when (clickType) {
            ClickType.PICKUP -> {
                val result = resultSlots.getItem(0)
                if (result.isEmpty) return
                val held = carried
                if (!held.isEmpty && !(ItemStack.isSameItemSameComponents(held, result) && held.count + result.count <= held.maxStackSize)) return
                if (!craft(player, storages)) return
                if (held.isEmpty) setCarried(result.copy()) else held.grow(result.count)
            }
            ClickType.QUICK_MOVE -> {
                var crafted = 0
                while (crafted < 64 && !resultSlots.getItem(0).isEmpty && player.inventory.freeSlot != -1) {
                    val result = resultSlots.getItem(0).copy()
                    if (!craft(player, storages)) break
                    player.inventory.placeItemBackInInventory(result)
                    crafted++
                }
            }
            else -> return
        }
        sync(true)
    }

    /** Uses up one craft's ingredients and returns the leftovers (e.g. empty buckets). False if they weren't all there. */
    private fun craft(player: ServerPlayer, storages: List<PlotStorage.Storage>): Boolean {
        if (!canSupply(player, storages)) return false
        val level = player.serverLevel()
        val input = craftSlots.asCraftInput()
        val result = resultSlots.getItem(0)
        CommonHooks.setCraftingPlayer(player)
        val remaining = level.recipeManager.getRemainingItemsFor(RecipeType.CRAFTING, input, level)
        CommonHooks.setCraftingPlayer(null)
        for (i in 0 until craftSlots.containerSize) {
            val template = craftSlots.getItem(i)
            if (template.isEmpty || !PlotStorage.extract(storages, template, 1).isEmpty) continue
            val slot = player.inventory.items.indexOfFirst { ItemStack.isSameItemSameComponents(it, template) }
            player.inventory.items[slot].shrink(1)
        }
        for (left in remaining) {
            if (left.isEmpty) continue
            val rest = insertIntoSelectedGroup(storages, left.copy())
            if (!rest.isEmpty) player.inventory.placeItemBackInInventory(rest)
        }
        result.onCraftedBy(level, player, result.count)
        EventHooks.firePlayerCraftingEvent(player, result, craftSlots)
        resultSlots.awardUsedRecipes(player, craftSlots.items)
        player.inventory.setChanged()
        updateResult()
        return true
    }

    private fun canSupply(player: ServerPlayer, storages: List<PlotStorage.Storage>): Boolean {
        val needed = ArrayList<Pair<ItemStack, Int>>()
        for (i in 0 until craftSlots.containerSize) {
            val template = craftSlots.getItem(i)
            if (template.isEmpty) continue
            val at = needed.indexOfFirst { ItemStack.isSameItemSameComponents(it.first, template) }
            if (at >= 0) needed[at] = needed[at].first to needed[at].second + 1 else needed.add(template to 1)
        }
        return needed.all { (template, amount) ->
            val inInventory = player.inventory.items.filter { ItemStack.isSameItemSameComponents(it, template) }.sumOf { it.count }
            PlotStorage.count(storages, template) + inInventory >= amount
        }
    }

    private val serverPlayer get() = inventory.player as? ServerPlayer

    private fun plot(): Pair<ServerLevel, Plot>? {
        val player = serverPlayer ?: return null
        val pocket = PlotManager.getPocketLevel(player.server) ?: return null
        val plot = PlotManager.get(pocket).get(plotId) ?: return null
        return pocket to plot
    }

    private fun storages(): List<PlotStorage.Storage>? {
        val (pocket, plot) = plot() ?: return null
        val all = PlotStorage.detect(pocket, plot)
        return PlotStorage.members(selectedGroup(plot), all)
    }

    /** Only valid after [PlotStorage.detect], which guarantees the plot has a group. */
    private fun selectedGroup(plot: Plot): StorageGroup = plot.groups[groupIndex.coerceIn(plot.groups.indices)]

    private fun insertIntoSelectedGroup(storages: List<PlotStorage.Storage>, stack: ItemStack): ItemStack {
        val (_, plot) = plot() ?: return stack
        return PlotStorage.insert(storages, stack, selectedGroup(plot).fillPartialStacksFirst)
    }

    fun groupAction(action: Int, index: Int, name: String = "") {
        val (pocket, plot) = plot() ?: return
        PlotStorage.detect(pocket, plot)
        when (action) {
            StorageGroupPayload.SELECT -> {
                if (index !in plot.groups.indices) return
                groupIndex = index
            }
            StorageGroupPayload.ADD -> {
                if (plot.groups.size >= ModConfig.maxGroups(plot.tier)) return
                plot.groups.add(StorageGroup(order = ArrayList(plot.groups.first().order)))
                groupIndex = plot.groups.lastIndex
            }
            StorageGroupPayload.REMOVE -> {
                if (plot.groups.size <= 1) return
                plot.groups.removeAt(groupIndex)
                groupIndex = groupIndex.coerceAtMost(plot.groups.lastIndex)
            }
            StorageGroupPayload.RENAME -> selectedGroup(plot).name = name.trim().take(StorageGroup.MAX_NAME)
            StorageGroupPayload.TOGGLE_FILL_PARTIAL_STACKS_FIRST -> {
                val group = selectedGroup(plot)
                group.fillPartialStacksFirst = !group.fillPartialStacksFirst
            }
            else -> return
        }
        PlotManager.get(pocket).setDirty()
        sync(true)
    }

    /** Placed backpacks are entered instantly; a worn one must be held for the configured time, restarting on damage. */
    private val enterHoldTicks get() = if (placed) 0 else ModConfig.wornEnterHoldTicks()
    private var enterHeld = false
    private var enterHeldTicks = 0

    fun setEnterHeld(held: Boolean) {
        enterHeld = held
        enterHeldTicks = 0
        if (held && enterHoldTicks == 0) enterBackpack()
    }

    private fun tickEnterHold(player: ServerPlayer) {
        if (!enterHeld) return
        enterHeldTicks = if (player.hurtTime > 0) 0 else enterHeldTicks + 1
        if (enterHeldTicks >= enterHoldTicks) enterBackpack()
    }

    private fun enterBackpack() {
        val player = inventory.player as? ServerPlayer ?: return
        val pocket = PlotManager.getPocketLevel(player.server) ?: return
        val plot = PlotManager.get(pocket).get(plotId) ?: return
        player.closeContainer()
        PlotTravel.enter(player, plot, pocket)
    }

    /** Moves items from the main inventory (the hotbar is left alone) into the selected group. */
    fun deposit(matchingOnly: Boolean) {
        val storages = storages() ?: return
        for (index in INVENTORY_START until HOTBAR_START) {
            val slot = slots[index]
            val stack = slot.item
            if (stack.isEmpty || (matchingOnly && PlotStorage.count(storages, stack) == 0L)) continue
            val rest = insertIntoSelectedGroup(storages, stack.copy())
            if (rest.count != stack.count) slot.set(rest)
        }
        sync(true)
    }

    /** A click on the aggregated grid: put the carried stack in, or take the clicked item out. */
    fun click(template: ItemStack, button: Int, shift: Boolean) {
        val player = serverPlayer ?: return
        val storages = storages() ?: return
        val carried = carried
        if (!carried.isEmpty) {
            val offered = if (button == 1) carried.copyWithCount(1) else carried.copy()
            val rest = insertIntoSelectedGroup(storages, offered)
            carried.shrink(offered.count - rest.count)
            setCarried(if (carried.isEmpty) ItemStack.EMPTY else carried)
        } else if (!template.isEmpty) {
            val available = PlotStorage.count(storages, template)
            if (available > 0) {
                val stackSize = minOf(available, template.maxStackSize.toLong()).toInt()
                val amount = if (button == 1) (stackSize + 1) / 2 else stackSize
                val taken = PlotStorage.extract(storages, template, amount)
                if (shift) {
                    player.inventory.placeItemBackInInventory(taken)
                } else {
                    setCarried(taken)
                }
            }
        }
        sync(true)
    }

    fun configure(index: Int, action: Int, target: Int) {
        val (pocket, plot) = plot() ?: return
        PlotStorage.detect(pocket, plot)
        val group = selectedGroup(plot)
        val order = group.order
        if (index !in order.indices) return
        when (action) {
            StorageConfigPayload.TOGGLE -> {
                val pos = order[index]
                if (!group.enabled.remove(pos)) {
                    if (group.enabled.size >= ModConfig.maxStoragesPerGroup(plot.tier)) return
                    group.enabled.add(pos)
                }
            }
            StorageConfigPayload.UP -> if (index > 0) java.util.Collections.swap(order, index, index - 1)
            StorageConfigPayload.DOWN -> if (index < order.size - 1) java.util.Collections.swap(order, index, index + 1)
            StorageConfigPayload.MOVE -> {
                if (target !in order.indices) return
                order.add(target, order.removeAt(index))
            }
            else -> return
        }
        PlotManager.get(pocket).setDirty()
        sync(true)
    }

    override fun clickMenuButton(player: Player, id: Int): Boolean {
        if (id != BUTTON_CLEAR_GRID) return false
        craftSlots.clearContent()
        return true
    }

    override fun slotsChanged(container: Container) {
        super.slotsChanged(container)
        if (container === craftSlots) updateResult()
    }

    private fun updateResult() {
        val player = serverPlayer ?: return
        val level = player.serverLevel()
        val input = craftSlots.asCraftInput()
        val holder = level.server.recipeManager.getRecipeFor(RecipeType.CRAFTING, input, level)
        var result = ItemStack.EMPTY
        if (holder.isPresent && resultSlots.setRecipeUsed(level, player, holder.get())) {
            val assembled = holder.get().value().assemble(input, level.registryAccess())
            val storages = storages()
            if (assembled.isItemEnabled(level.enabledFeatures()) && storages != null && canSupply(player, storages)) result = assembled
        }
        if (ItemStack.matches(result, resultSlots.getItem(0))) return
        resultSlots.setItem(0, result)
        setRemoteSlot(0, result)
        player.connection.send(ClientboundContainerSetSlotPacket(containerId, incrementStateId(), 0, result))
    }

    override fun canTakeItemForPickAll(stack: ItemStack, slot: Slot): Boolean =
        slot.container !== resultSlots && slot.container !== craftSlots && super.canTakeItemForPickAll(stack, slot)

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        val slot = slots.getOrNull(index) ?: return ItemStack.EMPTY
        if (index < INVENTORY_START || !slot.hasItem()) return ItemStack.EMPTY
        val stack = slot.item
        val copy = stack.copy()
        val storages = storages() ?: return ItemStack.EMPTY
        val rest = insertIntoSelectedGroup(storages, stack.copy())
        if (rest.count == stack.count) return ItemStack.EMPTY
        slot.set(if (rest.isEmpty) ItemStack.EMPTY else rest)
        sync(true)
        return copy
    }

    override fun stillValid(player: Player): Boolean {
        if (!player.isAlive) return false
        anchor?.let { return player.level().dimension() == it.dimension() && player.blockPosition().distSqr(it.pos()) < 100 }
        val worn = WornBackpack.find(player)
        return worn.item is BackpackItem && worn.get(ModComponents.PLOT_ID.get()) == plotId
    }

    override fun broadcastChanges() {
        super.broadcastChanges()
        val player = serverPlayer ?: return
        if (ticks % PlotLoader.REFRESH_INTERVAL == 0) plot()?.second?.let { PlotLoader.keepLoaded(player.server, it) }
        if (lastSent == null || ticks % 10 == 0) sync(false)
        if (ticks % 10 == 0) updateResult()
        tickEnterHold(player)
        ticks++
    }

    private fun sync(force: Boolean) {
        val player = serverPlayer ?: return
        val (pocket, plot) = plot() ?: return
        val all = PlotStorage.detect(pocket, plot)
        val group = selectedGroup(plot)
        val items = PlotStorage.totals(PlotStorage.members(group, all)).map { StorageSyncPayload.Item(it.stack, it.count) }
        val infos = group.order.map { pos ->
            val storage = all.getValue(pos)
            val size = storage.handler.slots
            val used = (0 until size).count { !storage.handler.getStackInSlot(it).isEmpty }
            StorageSyncPayload.Info(pos.subtract(plot.origin), blockIcon(storage), pos in group.enabled, used, size)
        }
        val groups = plot.groups.map { g ->
            val members = PlotStorage.members(g, all)
            val totalSlots = members.sumOf { it.handler.slots }
            val usedSlots = members.sumOf { m -> (0 until m.handler.slots).count { !m.handler.getStackInSlot(it).isEmpty } }
            StorageSyncPayload.Group(
                members.firstOrNull()?.let(::blockIcon) ?: ItemStack.EMPTY,
                members.size,
                g.name,
                usedSlots,
                totalSlots,
                g.fillPartialStacksFirst
            )
        }
        val selected = plot.groups.indexOf(group)
        val signature = listOf(
            selected,
            groups.map { listOf(it.icon.item, it.size, it.name, it.usedSlots, it.totalSlots, it.fillPartialStacksFirst) },
            items.map { ItemStack.hashItemAndComponents(it.stack) to it.count },
            infos.map { listOf(it.pos, it.enabled, it.used, it.size) },
            enterHoldTicks
        )
        if (!force && signature == lastSent) return
        lastSent = signature
        PacketDistributor.sendToPlayer(player, StorageSyncPayload(groups, selected, items, infos, enterHoldTicks))
    }

    private fun blockIcon(storage: PlotStorage.Storage) = ItemStack(storage.state.block.asItem())

    companion object {
        const val RESULT_SLOT = 0
        const val CRAFT_START = 1
        const val INVENTORY_START = 10
        const val HOTBAR_START = 37
        const val INVENTORY_END = 46

        /** Menu button id: move the crafting grid's contents into storage. */
        const val BUTTON_CLEAR_GRID = 0

        /** Opens the screen of the plot [plot], from the worn backpack ([anchor] null) or a placed one. */
        fun open(player: ServerPlayer, plot: Plot, anchor: GlobalPos?) {
            PlotLoader.keepLoaded(player.server, plot)
            player.openMenu(
                SimpleMenuProvider(
                    { id, inventory, _ -> StorageMenu(id, inventory, plot.id, anchor != null).also { it.anchor = anchor } },
                    Component.translatable("container.dimensional_backpacks.storage")
                )
            ) {
                it.writeVarInt(plot.id)
                it.writeBoolean(anchor != null)
            }
        }

        /** Opens the screen for the backpack the player is wearing. */
        fun openWorn(player: ServerPlayer) {
            if (player.containerMenu !== player.inventoryMenu) return
            val worn = WornBackpack.find(player)
            val plotId = worn.get(ModComponents.PLOT_ID.get())
            val plot = PlotManager.getPocketLevel(player.server)?.let { pocket -> plotId?.let { PlotManager.get(pocket).get(it) } }
            if (worn.item !is BackpackItem || plot == null) {
                player.displayClientMessage(Component.literal("You aren't wearing a backpack with storage"), true)
                return
            }
            open(player, plot, null)
        }
    }
}
