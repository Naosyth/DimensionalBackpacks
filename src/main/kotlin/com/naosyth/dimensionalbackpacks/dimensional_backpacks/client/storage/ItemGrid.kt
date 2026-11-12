package com.naosyth.dimensionalbackpacks.dimensional_backpacks.client.storage

import com.naosyth.dimensionalbackpacks.dimensional_backpacks.network.StorageSyncPayload.Item as Entry
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.screens.Screen.hasShiftDown
import net.minecraft.client.gui.screens.Screen.hasControlDown
import net.minecraft.world.item.ItemStack

/** The aggregated items of the selected group as a searchable, sortable grid of slots. */
internal class ItemGrid(
    content: Rect,
    track: Rect,
    private val cols: Int,
    private val model: StorageModel,
    private val actions: StorageActions,
    private val carried: () -> ItemStack
) : ScrollView(content, track) {

    override val rowHeight = CELL
    override val totalRows get() = (shown.size + cols - 1) / cols

    var query = ""
        set(value) {
            field = value
            resetScroll()
            refresh()
        }

    /** Entries after search and sort. */
    private var shown: List<Entry> = emptyList()

    // An item taken to the cursor may empty its storage; its cell stays (count 0) until the cursor lets go of it.
    private var pinned: ItemStack = ItemStack.EMPTY
    private var pinnedSeenOnCursor = false
    private var pinnedAge = 0

    private var wasModifierHeld = false

    // Frozen during modifier extraction until both modifiers are released.
    private var frozen = false

    // ---- Data ----

    /** Rebuilds [shown]; call after the model, the query or the sort changes. */
    fun refresh() {
        if (frozen && shown.isNotEmpty() && refreshInPlace()) return
        val search = query.trim().lowercase()
        val name = { e: Entry -> e.stack.hoverName.string.lowercase() }
        val order: Comparator<Entry> = when (StoragePrefs.sortBy) {
            SortBy.NAME -> compareBy(name)
            SortBy.COUNT -> compareBy<Entry> { it.count }.thenBy(name)
        }
        shown = itemsWithPinned().filter { matches(it.stack, search) }
            .sortedWith(if (StoragePrefs.ascending) order else order.reversed())
    }

    /** While frozen, counts update and vanished items stay at 0. */
    private fun refreshInPlace(): Boolean {
        val kept = shown.map { old ->
            val now = model.items.firstOrNull { ItemStack.isSameItemSameComponents(it.stack, old.stack) }
            Entry(old.stack, now?.count ?: 0)
        }
        val search = query.trim().lowercase()
        val hasNewItems = model.items.any { item ->
            matches(item.stack, search) && kept.none { ItemStack.isSameItemSameComponents(it.stack, item.stack) }
        }
        if (hasNewItems) return false
        shown = kept
        return true
    }

    private fun itemsWithPinned(): List<Entry> {
        val items = model.items
        if (pinned.isEmpty || items.any { ItemStack.isSameItemSameComponents(it.stack, pinned) }) return items
        return items + Entry(pinned, 0)
    }

    private fun matches(stack: ItemStack, search: String): Boolean = when {
        search.isEmpty() -> true
        search.startsWith("@") -> BuiltInRegistries.ITEM.getKey(stack.item).namespace.contains(search.drop(1))
        search.startsWith("#") -> UiDraw.itemTooltip(stack).any { it.string.lowercase().contains(search.drop(1)) }
        else -> stack.hoverName.string.lowercase().contains(search)
    }

    fun tick() {
        val modifierHeld = hasShiftDown() || hasControlDown()
        if (wasModifierHeld && !modifierHeld) {
            frozen = false
            pinned = ItemStack.EMPTY
            refresh()
        }
        wasModifierHeld = modifierHeld
        if (pinned.isEmpty) return
        val onCursor = carried().takeIf { !it.isEmpty && ItemStack.isSameItemSameComponents(it, pinned) }
        if (onCursor != null) {
            pinnedSeenOnCursor = true
        } else if (pinnedSeenOnCursor || ++pinnedAge > PIN_TIMEOUT_TICKS) {
            pinned = ItemStack.EMPTY
            refresh()
        }
    }

    private fun pin(stack: ItemStack) {
        pinned = stack.copyWithCount(1)
        pinnedSeenOnCursor = false
        pinnedAge = 0
    }

    // ---- Drawing ----

    override fun renderContent(g: GuiGraphics, mx: Int, my: Int) {
        for (row in 0 until visibleRows) for (col in 0 until cols) UiDraw.slot(g, content.x + col * CELL, content.y + row * CELL)
        if (shown.isEmpty()) {
            renderEmptyMessage(g)
        } else {
            for (row in 0 until visibleRows) for (col in 0 until cols) {
                val entry = shown.getOrNull((row + scroll) * cols + col) ?: break
                val x = content.x + col * CELL + 1
                val y = content.y + row * CELL + 1
                g.renderItem(entry.stack, x, y)
                g.renderItemDecorations(UiDraw.font, entry.stack, x, y, "")
                if (entry.count != 1L) UiDraw.cornerText(g, formatCount(entry.count), x, y)
            }
        }
        if (content.contains(mx, my)) {
            val col = (mx - content.x) / CELL
            val row = (my - content.y) / CELL
            if (col < cols) renderHoverHighlight(g, content.x + col * CELL + 1, content.y + row * CELL + 1)
        }
    }

    /** Drawn above the item (like vanilla), so it needs a z above the item's depth. */
    private fun renderHoverHighlight(g: GuiGraphics, x: Int, y: Int) {
        g.pose().pushPose()
        g.pose().translate(0f, 0f, 200f)
        g.fill(x, y, x + 16, y + 16, 0x80FFFFFF.toInt())
        g.pose().popPose()
    }

    private fun renderEmptyMessage(g: GuiGraphics) {
        val text = when {
            !model.received -> "Loading..."
            model.storages.isEmpty() -> "No storage found. Place chests inside the backpack."
            model.storages.none { it.enabled } -> "No storages in this group. Use the gear to add some."
            else -> return
        }
        val font = UiDraw.font
        val lines = font.split(Component.literal(text), content.w - 24)
        lines.forEachIndexed { i, line ->
            g.drawString(font, line, content.centerX - font.width(line) / 2, content.centerY - lines.size * 5 + i * 10, UiDraw.TEXT_LIGHT, true)
        }
    }

    private fun formatCount(count: Long) = when {
        count < 1_000 -> count.toString()
        count < 1_000_000 -> "%.1fK".format(count / 1_000.0)
        else -> "%.1fM".format(count / 1_000_000.0)
    }

    // ---- Input ----

    /** The entry under the mouse and its (column, visible row), or null over an empty cell. */
    private fun entryAt(mx: Int, my: Int): Pair<Int, Int>? {
        if (!content.contains(mx, my)) return null
        val col = (mx - content.x) / CELL
        val row = (my - content.y) / CELL
        return (col to row).takeIf { shown.getOrNull((row + scroll) * cols + col) != null }
    }

    private fun stackAt(mx: Int, my: Int): ItemStack? =
        entryAt(mx, my)?.let { (col, row) -> shown[(row + scroll) * cols + col].stack }

    fun ingredientAt(mx: Int, my: Int): Pair<ItemStack, Rect>? {
        val (col, row) = entryAt(mx, my) ?: return null
        val entry = shown[(row + scroll) * cols + col]
        if (entry.count <= 0) return null
        return entry.stack to Rect(content.x + col * CELL, content.y + row * CELL, CELL, CELL)
    }

    private var shiftDragging = false
    private var lastDragIndex = -1

    private fun indexAt(mx: Int, my: Int): Int? =
        entryAt(mx, my)?.let { (col, row) -> (row + scroll) * cols + col }

    override fun contentClicked(mx: Int, my: Int, button: Int): Boolean {
        if (button != 0 && button != 1) return false
        val template = stackAt(mx, my) ?: ItemStack.EMPTY
        val control = button == 0 && hasControlDown() && carried().isEmpty
        val shift = hasShiftDown() && !control
        if (carried().isEmpty && !template.isEmpty && !shift && !control) pin(template)
        shiftDragging = button == 0 && shift
        lastDragIndex = indexAt(mx, my) ?: -1
        if ((shift || control) && carried().isEmpty && !template.isEmpty) frozen = true
        actions.click(template, button, shift, control)
        return true
    }

    /** Holding Shift and dragging across cells shift-extracts from each newly entered one. */
    override fun mouseDragged(mx: Int, my: Int): Boolean {
        if (super.mouseDragged(mx, my)) return true
        if (!shiftDragging) return false
        if (!hasShiftDown()) { shiftDragging = false; return false }
        val index = indexAt(mx, my) ?: return true
        if (index != lastDragIndex) {
            lastDragIndex = index
            frozen = true
            actions.click(shown[index].stack, 0, true)
        }
        return true
    }

    override fun mouseReleased(mx: Int, my: Int): Boolean {
        val wasDragging = shiftDragging
        shiftDragging = false
        return super.mouseReleased(mx, my) || wasDragging
    }

    override fun tooltip(mx: Int, my: Int): List<Component>? {
        if (!carried().isEmpty) return null
        val (col, row) = entryAt(mx, my) ?: return null
        val entry = shown[(row + scroll) * cols + col]
        return UiDraw.itemTooltip(entry.stack) + Component.literal("Total: ${"%,d".format(entry.count)}").withStyle(ChatFormatting.AQUA)
    }

    private companion object {
        const val CELL = 18
        const val PIN_TIMEOUT_TICKS = 40
    }
}
