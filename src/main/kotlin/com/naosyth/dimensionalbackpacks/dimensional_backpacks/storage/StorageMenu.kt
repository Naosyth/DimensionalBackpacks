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
import net.minecraft.core.GlobalPos
import net.minecraft.core.NonNullList
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Container
import net.minecraft.world.InteractionHand
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ResultContainer
import net.minecraft.world.inventory.ResultSlot
import net.minecraft.world.inventory.Slot
import net.minecraft.world.inventory.TransientCraftingContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.RecipeType
import net.neoforged.neoforge.network.PacketDistributor

/**
 * The aggregated storage screen of a plot. The player's inventory is made of normal slots; the
 * aggregated item grid is virtual, driven by custom click packets, and synced with [StorageSyncPayload].
 * The crafting grid holds real items shared by everyone accessing the same backpack.
 *
 * Slot indices: 0 result, 1-9 crafting grid, 10-36 inventory, 37-45 hotbar. Slot positions are
 * laid out by the client screen.
 */
class StorageMenu(containerId: Int, private val inventory: Inventory, val plotId: Int, val placed: Boolean = false) :
    AbstractContainerMenu(ModMenus.STORAGE.get(), containerId) {

    /** Set on the server when opened from a placed backpack; the menu closes if the player walks away. */
    var anchor: GlobalPos? = null
    private var heldHand: InteractionHand? = null
    private var heldSlot: Int = -1

    private val craftSlots = object : TransientCraftingContainer(
        this, 3, 3, plot()?.second?.craftingItems ?: NonNullList.withSize(9, ItemStack.EMPTY)
    ) {
        override fun setChanged() {
            this@StorageMenu.slotsChanged(this)
        }
    }
    private val resultSlots = ResultContainer()
    private var updatingCraftSlots = false
    private var ticks = 0
    private var lastSent: List<Any>? = null
    private var groupIndex = 0

    init {
        addSlot(object : ResultSlot(inventory.player, craftSlots, resultSlots, 0, 0, 0) {
            override fun onTake(player: Player, stack: ItemStack) {
                if (player !is ServerPlayer) {
                    super.onTake(player, stack)
                    return
                }
                val ingredients = craftSlots.items.map { it.copy() }
                updatingCraftSlots = true
                try {
                    super.onTake(player, stack)
                    refillCraftSlots(ingredients)
                } finally {
                    updatingCraftSlots = false
                    slotsChanged(craftSlots)
                    sync(true)
                }
            }
        })
        for (i in 0 until 9) addSlot(Slot(craftSlots, i, 0, 0))
        for (row in 0 until 3) {
            for (col in 0 until 9) addSlot(Slot(inventory, col + row * 9 + 9, 0, 0))
        }
        for (col in 0 until 9) addSlot(Slot(inventory, col, 0, 0))
    }

    fun transferRecipe(templates: List<ItemStack>, maxTransfer: Boolean) {
        val player = serverPlayer ?: return
        if (templates.size != craftSlots.containerSize) return
        val storages = storages() ?: return
        val groups = ArrayList<RecipeIngredient>()
        templates.forEachIndexed { index, stack ->
            if (!stack.isEmpty) {
                val group = groups.firstOrNull { ItemStack.isSameItemSameComponents(it.template, stack) }
                    ?: RecipeIngredient(stack.copyWithCount(1)).also { groups.add(it) }
                group.slots.add(index)
            }
        }
        if (groups.isEmpty()) return
        val totals = PlotStorage.totals(storages)
        val grid = craftSlots.items.map { it.copy() }
        val sets = maximumRecipeSets(groups.map { group ->
            val stored = totals.firstOrNull { ItemStack.isSameItemSameComponents(it.stack, group.template) }?.count ?: 0L
            val inInventory = player.inventory.items.filter { ItemStack.isSameItemSameComponents(it, group.template) }.sumOf { it.count.toLong() }
            val inGrid = grid.filter { ItemStack.isSameItemSameComponents(it, group.template) }.sumOf { it.count.toLong() }
            RecipeSupply(group.slots.size, stored + inInventory + inGrid, group.template.maxStackSize)
        })
        if (sets == 0) {
            player.displayClientMessage(Component.literal("Not enough ingredients to transfer this recipe."), true)
            return
        }
        val countPerSlot = if (maxTransfer) sets else 1
        val reserved = ArrayList<ItemStack>()
        var committed = false
        updatingCraftSlots = true
        try {
            for (group in groups) {
                val inGrid = grid.filter { ItemStack.isSameItemSameComponents(it, group.template) }.sumOf { it.count }
                group.remaining = (group.slots.size * countPerSlot - inGrid).coerceAtLeast(0)
            }
            if (!reserveStorageIngredients(storages.map { it.handler }, groups, reserved)) {
                player.displayClientMessage(Component.literal("Storage returned unexpected ingredients; recipe transfer cancelled."), true)
                return
            }
            for (stack in player.inventory.items) {
                val group = groups.firstOrNull { it.remaining > 0 && ItemStack.isSameItemSameComponents(it.template, stack) }
                    ?: continue
                val taken = stack.split(minOf(group.remaining, stack.count))
                if (!taken.isEmpty) {
                    reserved.add(taken)
                    group.remaining -= taken.count
                }
            }
            if (groups.any { it.remaining > 0 }) {
                player.displayClientMessage(Component.literal("Not enough extractable ingredients; recipe transfer cancelled."), true)
                return
            }

            val pool = (grid.filter { !it.isEmpty } + reserved.map { it.copy() }).toMutableList()
            val replacement = MutableList(craftSlots.containerSize) { ItemStack.EMPTY }
            for (group in groups) for (slot in group.slots) {
                var needed = countPerSlot
                val result = group.template.copyWithCount(countPerSlot)
                for (stack in pool) {
                    if (needed == 0) break
                    if (!ItemStack.isSameItemSameComponents(stack, group.template)) continue
                    val amount = minOf(needed, stack.count)
                    stack.shrink(amount)
                    needed -= amount
                }
                check(needed == 0) { "Reserved crafting ingredients were lost" }
                replacement[slot] = result
            }
            replacement.forEachIndexed { index, stack -> craftSlots.setItem(index, stack) }
            committed = true
            pool.filter { !it.isEmpty }.forEach { player.inventory.placeItemBackInInventory(it) }
        } finally {
            updatingCraftSlots = false
            if (!committed) reserved.filter { !it.isEmpty }.forEach { player.inventory.placeItemBackInInventory(it) }
            player.inventory.setChanged()
            slotsChanged(craftSlots)
            sync(true)
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

    private fun refillCraftSlots(ingredients: List<ItemStack>) {
        if (ingredients.indices.none { !ingredients[it].isEmpty && craftSlots.getItem(it).isEmpty }) return
        val storages = storages() ?: return
        ingredients.forEachIndexed { index, ingredient ->
            if (ingredient.isEmpty || !craftSlots.getItem(index).isEmpty) return@forEachIndexed
            val replacement = PlotStorage.extract(storages, ingredient, 1)
            if (!replacement.isEmpty) craftSlots.setItem(index, replacement)
        }
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
        if (held && heldHand != null) {
            serverPlayer?.displayClientMessage(Component.literal("Wear or place the backpack before entering it"), true)
            return
        }
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
    fun click(template: ItemStack, button: Int, shift: Boolean, control: Boolean) {
        val player = serverPlayer ?: return
        if (button !in 0..1) return
        val storages = storages() ?: return
        val carried = carried
        if (!carried.isEmpty) {
            val offered = if (button == 1) carried.copyWithCount(1) else carried.copy()
            val rest = insertIntoSelectedGroup(storages, offered)
            carried.shrink(offered.count - rest.count)
            setCarried(if (carried.isEmpty) ItemStack.EMPTY else carried)
        } else if (!template.isEmpty) {
            if (control && button == 0) {
                val inventory = player.inventory
                val hasSpace = inventory.getFreeSlot() >= 0 ||
                    (!template.isDamaged && inventory.getSlotWithRemainingSpace(template) >= 0)
                if (!hasSpace) {
                    player.displayClientMessage(Component.literal("Your inventory is full."), true)
                    return
                }
                val taken = PlotStorage.extract(storages, template, 1)
                if (!taken.isEmpty) inventory.placeItemBackInInventory(taken)
                sync(true)
                return
            }
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
        if (player !is ServerPlayer) return true
        val storages = storages() ?: return false
        updatingCraftSlots = true
        try {
            for (i in 0 until craftSlots.containerSize) {
                val stack = craftSlots.removeItemNoUpdate(i)
                if (stack.isEmpty) continue
                val remaining = insertIntoSelectedGroup(storages, stack)
                if (!remaining.isEmpty) player.inventory.placeItemBackInInventory(remaining)
            }
        } finally {
            updatingCraftSlots = false
        }
        slotsChanged(craftSlots)
        sync(true)
        return true
    }

    override fun slotsChanged(container: Container) {
        if (container === craftSlots) {
            if (updatingCraftSlots) return
            val player = serverPlayer
            if (player != null) {
                plot()?.first?.let { PlotManager.get(it).setDirty() }
                for (other in player.server.playerList.players) {
                    val menu = other.containerMenu as? StorageMenu ?: continue
                    if (menu.plotId == plotId && menu !== this) menu.updateResult()
                }
            }
            updateResult()
        }
        super.broadcastChanges()
    }

    private fun updateResult() {
        val player = serverPlayer ?: return
        val level = player.serverLevel()
        val input = craftSlots.asCraftInput()
        val holder = level.server.recipeManager.getRecipeFor(RecipeType.CRAFTING, input, level)
        var result = ItemStack.EMPTY
        if (holder.isPresent && resultSlots.setRecipeUsed(level, player, holder.get())) {
            val assembled = holder.get().value().assemble(input, level.registryAccess())
            if (assembled.isItemEnabled(level.enabledFeatures())) result = assembled
        }
        resultSlots.setItem(0, result)
        setRemoteSlot(0, result)
        player.connection.send(ClientboundContainerSetSlotPacket(containerId, incrementStateId(), 0, result))
    }

    override fun canTakeItemForPickAll(stack: ItemStack, slot: Slot): Boolean =
        slot.container !== resultSlots && super.canTakeItemForPickAll(stack, slot)

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        val slot = slots.getOrNull(index) ?: return ItemStack.EMPTY
        if (!slot.hasItem()) return ItemStack.EMPTY
        val stack = slot.item
        val copy = stack.copy()
        if (index == RESULT_SLOT) {
            if (!moveItemStackTo(stack, INVENTORY_START, INVENTORY_END, true)) return ItemStack.EMPTY
            slot.onQuickCraft(stack, copy)
            if (stack.isEmpty) slot.setByPlayer(ItemStack.EMPTY) else slot.setChanged()
            if (stack.count == copy.count) return ItemStack.EMPTY
            slot.onTake(player, stack)
            player.drop(stack, false)
            return copy
        }
        if (index in CRAFT_START until INVENTORY_START) {
            val storages = storages() ?: return ItemStack.EMPTY
            val rest = insertIntoSelectedGroup(storages, stack.copy())
            if (!rest.isEmpty) moveItemStackTo(rest, INVENTORY_START, INVENTORY_END, false)
            if (rest.count == copy.count) return ItemStack.EMPTY
            slot.setByPlayer(rest)
            slot.onTake(player, rest)
            sync(true)
            return copy
        }
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
        heldHand?.let { hand ->
            if (hand == InteractionHand.MAIN_HAND && player.inventory.selected != heldSlot) return false
            val stack = player.getItemInHand(hand)
            return stack.item is BackpackItem && stack.get(ModComponents.PLOT_ID.get()) == plotId
        }
        val worn = WornBackpack.find(player)
        return worn.item is BackpackItem && worn.get(ModComponents.PLOT_ID.get()) == plotId
    }

    override fun broadcastChanges() {
        super.broadcastChanges()
        val player = serverPlayer ?: return
        if (ticks == 0) updateResult()
        if (ticks % PlotLoader.REFRESH_INTERVAL == 0) plot()?.second?.let { PlotLoader.keepLoaded(player.server, it) }
        if (lastSent == null || ticks % 10 == 0) sync(false)
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
            val preview = (0 until minOf(size, StorageSyncPayload.Info.PREVIEW_SLOTS))
                .map { storage.handler.getStackInSlot(it).copy() }
                .dropLastWhile { it.isEmpty }
            StorageSyncPayload.Info(pos.subtract(plot.origin), blockIcon(storage), pos in group.enabled, used, size, preview)
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
            infos.map { info -> listOf(info.pos, info.enabled, info.used, info.size) + info.preview.map { ItemStack.hashItemAndComponents(it) to it.count } },
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

        /** Opens the screen of a worn, held or placed backpack. */
        fun open(player: ServerPlayer, plot: Plot, anchor: GlobalPos?, hand: InteractionHand? = null) {
            PlotLoader.keepLoaded(player.server, plot)
            player.openMenu(
                SimpleMenuProvider(
                    { id, inventory, _ -> StorageMenu(id, inventory, plot.id, anchor != null).also {
                        it.anchor = anchor
                        it.heldHand = hand
                        it.heldSlot = if (hand == InteractionHand.MAIN_HAND) player.inventory.selected else -1
                    } },
                    Component.translatable("container.dimensional_backpacks.storage")
                )
            ) {
                it.writeVarInt(plot.id)
                it.writeBoolean(anchor != null)
            }
        }

        fun openHeld(player: ServerPlayer, hand: InteractionHand): Boolean {
            if (player.containerMenu !== player.inventoryMenu) return false
            val stack = player.getItemInHand(hand)
            val item = stack.item as? BackpackItem ?: return false
            val pocket = PlotManager.getPocketLevel(player.server)
            if (pocket == null) {
                player.displayClientMessage(Component.literal("The pocket dimension is not available"), true)
                return false
            }
            val id = stack.get(ModComponents.PLOT_ID.get())
            val plot = if (id == null) {
                val created = PlotManager.allocate(player.server, item.tier)
                if (created == null) {
                    player.displayClientMessage(Component.literal("Could not create the backpack interior"), true)
                    return false
                }
                stack.set(ModComponents.PLOT_ID.get(), created.id)
                PlotManager.get(pocket).setColor(created, stack.get(DataComponents.DYED_COLOR)?.rgb, pocket)
                created
            } else {
                PlotManager.get(pocket).get(id) ?: run {
                    player.displayClientMessage(Component.literal("This backpack's interior could not be found"), true)
                    return false
                }
            }
            open(player, plot, null, hand)
            return true
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
