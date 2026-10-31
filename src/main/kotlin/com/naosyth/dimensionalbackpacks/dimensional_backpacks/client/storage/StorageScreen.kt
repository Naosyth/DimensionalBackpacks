package com.naosyth.dimensionalbackpacks.dimensional_backpacks.client.storage

import com.naosyth.dimensionalbackpacks.dimensional_backpacks.client.ClientInit
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.mixin.SlotAccessor
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.network.StorageSyncPayload
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.storage.StorageMenu
import com.mojang.blaze3d.platform.InputConstants
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.StorageGroup
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.Slot
import java.util.Optional
import kotlin.math.sign

/**
 * The backpack's storage screen. Top to bottom: a group bar, a search and sort toolbar, the item grid, and a
 * crafting panel beside the player's inventory. The gear in the group bar swaps the toolbar, grid and bottom
 * panels for a configuration list of the selected group's storages.
 */
class StorageScreen(menu: StorageMenu, inventory: Inventory, title: Component) :
    AbstractContainerScreen<StorageMenu>(menu, inventory, title) {

    private val model = StorageModel()
    private val actions = StorageActions(menu)
    private var configMode = false

    private lateinit var layout: Layout
    private lateinit var search: EditBox
    private lateinit var nameBox: EditBox
    private val textBoxes get() = listOf(search, nameBox).filter { it.visible }
    private var renaming = false
    private lateinit var grid: ItemGrid
    private lateinit var mainViews: List<View>
    private lateinit var configViews: List<View>
    private val views get() = if (configMode) configViews else mainViews

    private var query = ""

    /** Absolute positions of everything, for a screen of the current size. */
    private class Layout(val x: Int, val y: Int, val rows: Int) {
        val storage = Rect(x, y, STORAGE_W, GRID_Y + rows * CELL + PAD)
        val groupBar = Rect(x + PAD, y + PAD, INNER_W, GroupBar.TAB)
        val gear = Rect(groupBar.right - GroupBar.TAB, groupBar.y, GroupBar.TAB, GroupBar.TAB)
        val toolbar = Rect(x + PAD, y + TOOLBAR_Y, INNER_W, TOOLBAR_H)
        val gridContent = Rect(x + PAD, y + GRID_Y, COLS * CELL, rows * CELL)
        val gridTrack = Rect(gridContent.right + 2, gridContent.y, SCROLL_W, gridContent.h)
        val crafting = Rect(x, storage.bottom + PANEL_GAP, STORAGE_W - PANEL_GAP - INV_W, BOTTOM_H)
        val inventory = Rect(storage.right - INV_W, crafting.y, INV_W, BOTTOM_H)
        val config = Rect(x, y, STORAGE_W, GRID_Y + rows * CELL + PAD)

        // A floating panel beside the screen, only used for placed backpacks.
        val enterPanel = Rect(storage.right + PANEL_GAP, y, ENTER_W, TOOLBAR_H + 2 * PAD)
        val enterButton = Rect(enterPanel.x + PAD, enterPanel.y + PAD, ENTER_W - 2 * PAD, TOOLBAR_H)

        val depositAll = Rect(crafting.x + PAD, crafting.y + PAD, DEPOSIT_W, DEPOSIT_H)
        val depositMatching = Rect(crafting.right - PAD - DEPOSIT_W, crafting.y + PAD, DEPOSIT_W, DEPOSIT_H)

        // The three crafting parts are centred horizontally, below the deposit buttons.
        val craftGrid = Rect(crafting.x + (crafting.w - CRAFT_W) / 2, crafting.y + CRAFT_Y, 3 * CELL, 3 * CELL)
        val craftArrow = Rect(craftGrid.right + 4, craftGrid.centerY - 5, 14, 10)
        val resultSlot = Rect(craftGrid.right + 22, craftGrid.centerY - CELL / 2, CELL, CELL)

        val sort = Rect(toolbar.right - SORT_W, toolbar.y, SORT_W, TOOLBAR_H)
        val search = Rect(toolbar.x, toolbar.y, sort.x - 4 - toolbar.x, 15)
        val fullnessBar = Rect(toolbar.x, toolbar.y + 17, sort.x - 4 - toolbar.x, TOOLBAR_H - 17)

        val nameField = Rect(toolbar.x, toolbar.y, toolbar.w - DELETE_W - 4, 15)
        val deleteGroup = Rect(toolbar.right - DELETE_W, toolbar.y, DELETE_W, TOOLBAR_H)

        val height get() = storage.h + PANEL_GAP + BOTTOM_H

        companion object {
            const val PAD = 7
            const val CELL = 18
            const val COLS = 15
            const val SCROLL_W = 10
            const val STORAGE_W = 2 * PAD + COLS * CELL + 2 + SCROLL_W
            const val INNER_W = STORAGE_W - 2 * PAD
            const val TOOLBAR_Y = PAD + GroupBar.TAB + 4
            const val TOOLBAR_H = 20
            const val GRID_Y = TOOLBAR_Y + TOOLBAR_H + 4
            const val INV_W = 176
            const val BOTTOM_H = 90
            const val PANEL_GAP = 2
            const val CRAFT_W = 3 * CELL + 22 + CELL
            const val SORT_W = 20
            const val DELETE_W = 20
            const val DEPOSIT_W = 51
            const val ENTER_W = 104
            const val CRAFT_Y = 27
            const val MIN_ROWS = 3
            const val MAX_ROWS = 12
            const val DEPOSIT_H = 14
            private const val SCREEN_MARGIN = 8

            fun rowsFor(screenHeight: Int): Int {
                val fixed = GRID_Y + PAD + PANEL_GAP + BOTTOM_H + SCREEN_MARGIN
                return ((screenHeight - fixed) / CELL).coerceIn(MIN_ROWS, MAX_ROWS)
            }
        }
    }

    // ---- Setup ----

    override fun init() {
        val rows = Layout.rowsFor(height)
        imageWidth = Layout.STORAGE_W
        imageHeight = Layout.GRID_Y + rows * Layout.CELL + Layout.PAD + Layout.PANEL_GAP + Layout.BOTTOM_H
        super.init()
        layout = Layout(leftPos, topPos, rows)

        grid = ItemGrid(layout.gridContent, layout.gridTrack, Layout.COLS, model, actions) { menu.carried }
        grid.query = query
        search = createSearchBox()
        search.visible = !configMode
        addRenderableWidget(search)
        nameBox = createNameBox()
        nameBox.visible = configMode
        addRenderableWidget(nameBox)
        showGroupName()

        val groupBar = GroupBar(layout.groupBar, model, actions) { configMode }
        val gear = Button(layout.gear, { g, r -> UiDraw.icon(g, UiDraw.IconType.GEAR, r) },
            { listOf(Component.literal(if (configMode) "Back to items" else "Configure groups")) },
            active = { configMode }) { toggleConfig() }
        val list = StorageList(
            Rect(layout.gridContent.x, layout.gridContent.y, layout.gridContent.w, layout.gridContent.h),
            layout.gridTrack, model, actions
        )

        val enter = Button(layout.enterButton, { g, r -> UiDraw.centeredText(g, "Enter Backpack", r, 0xFFFFFFFF.toInt()) },
            { listOf(Component.literal("Step inside this backpack")) },
            visible = { menu.placed }) { actions.enterBackpack() }

        mainViews = listOf(groupBar, gear, enter, sortButton(), depositButton(layout.depositAll, "All", "Deposit inventory",
            "Moves everything from your inventory into this group.", false),
            depositButton(layout.depositMatching, "Matching", "Deposit matching",
                "Moves items from your inventory that this group already holds.", true), grid)
        configViews = listOf(groupBar, gear, enter,
            Button(layout.deleteGroup, { g, r -> UiDraw.icon(g, UiDraw.IconType.TRASH, r) },
                { listOf(Component.literal("Delete this group")) },
                visible = { model.groups.size > 1 }) { actions.removeGroup() },
            list)
        selectedGroupListeners = listOf(list::resetScroll, grid::resetScroll)
        placeSlots()
    }

    private var selectedGroupListeners: List<() -> Unit> = emptyList()

    private fun createSearchBox() = EditBox(font, layout.search.x + 4, layout.search.y + 3, layout.search.w - SEARCH_ICON_W - 5, layout.search.h,
        Component.literal("Search")).apply {
        isBordered = false
        setMaxLength(50)
        setHint(Component.literal("Search..."))
        setValue(query)
        setResponder { query = it; grid.query = it }
    }

    private fun createNameBox() = EditBox(font, layout.nameField.x + 4, layout.nameField.y + 3, layout.nameField.w - 8, layout.nameField.h,
        Component.literal("Group name")).apply {
        isBordered = false
        setMaxLength(StorageGroup.MAX_NAME)
        setResponder { if (!renaming) actions.renameGroup(it) }
    }

    /** Shows the selected group's own name (not the "Group N" fallback) and its default as the hint. */
    private fun showGroupName() {
        renaming = true
        nameBox.setHint(Component.literal(model.groupName(model.selected)))
        nameBox.value = model.groups.getOrNull(model.selected)?.name.orEmpty()
        renaming = false
    }

    private fun sortButton() = Button(
        layout.sort,
        { g, r ->
            val iconType = when {
                StoragePrefs.sortBy == SortBy.NAME && StoragePrefs.ascending -> UiDraw.IconType.SORT_AZ
                StoragePrefs.sortBy == SortBy.NAME -> UiDraw.IconType.SORT_ZA
                StoragePrefs.ascending -> UiDraw.IconType.SORT_09
                else -> UiDraw.IconType.SORT_90
            }
            UiDraw.icon(g, iconType, r.centerX, r.centerY, 14)
        },
        {
            val name = when {
                StoragePrefs.sortBy == SortBy.NAME && StoragePrefs.ascending -> "Name, A to Z"
                StoragePrefs.sortBy == SortBy.NAME -> "Name, Z to A"
                StoragePrefs.ascending -> "Amount, lowest first"
                else -> "Amount, highest first"
            }
            listOf(Component.literal("Sort: $name"), Component.literal("Click to change").withStyle(ChatFormatting.GRAY))
        }
    ) {
        cycleSort()
        grid.refresh()
    }

    /** Name ascending, name descending, amount ascending, amount descending, then around again. */
    private fun cycleSort() {
        val byName = StoragePrefs.sortBy == SortBy.NAME
        when {
            byName && StoragePrefs.ascending -> StoragePrefs.ascending = false
            byName -> { StoragePrefs.sortBy = SortBy.COUNT; StoragePrefs.ascending = true }
            StoragePrefs.ascending -> StoragePrefs.ascending = false
            else -> { StoragePrefs.sortBy = SortBy.NAME; StoragePrefs.ascending = true }
        }
    }

    private fun depositButton(area: Rect, label: String, title: String, detail: String, matchingOnly: Boolean) = Button(
        area,
        { g, r ->
            UiDraw.centeredText(g, label, r, 0xFFFFFFFF.toInt())
        },
        { listOf(Component.literal(title), Component.literal(detail).withStyle(ChatFormatting.GRAY)) }
    ) { actions.deposit(matchingOnly) }

    private fun toggleConfig() {
        configMode = !configMode
        search.visible = !configMode
        nameBox.visible = configMode
        search.isFocused = false
        nameBox.isFocused = false
        showGroupName()
        placeSlots()
    }

    /** Menu slots follow the layout; in config mode they move off-screen so they can't be hovered or clicked. */
    private fun placeSlots() {
        val menu = menu
        fun place(slot: Slot, boxX: Int, boxY: Int) {
            val accessor = slot as SlotAccessor
            accessor.`dimensionalbackpacks$setX`(if (configMode) OFFSCREEN else boxX + 1 - leftPos)
            accessor.`dimensionalbackpacks$setY`(if (configMode) OFFSCREEN else boxY + 1 - topPos)
        }
        place(menu.slots[StorageMenu.RESULT_SLOT], layout.resultSlot.x, layout.resultSlot.y)
        for (i in 0 until 9) {
            place(menu.slots[StorageMenu.CRAFT_START + i],
                layout.craftGrid.x + (i % 3) * Layout.CELL, layout.craftGrid.y + (i / 3) * Layout.CELL)
        }
        val invX = layout.inventory.x + Layout.PAD
        val invY = layout.inventory.y + Layout.PAD
        for (i in 0 until 27) {
            place(menu.slots[StorageMenu.INVENTORY_START + i], invX + (i % 9) * Layout.CELL, invY + (i / 9) * Layout.CELL)
        }
        for (i in 0 until 9) {
            place(menu.slots[StorageMenu.HOTBAR_START + i], invX + i * Layout.CELL, invY + 3 * Layout.CELL + 4)
        }
    }

    // ---- Server updates ----

    fun update(payload: StorageSyncPayload) {
        val previous = model.selected
        val previousCount = model.groups.size
        model.update(payload)
        if (model.selected != previous || model.groups.size != previousCount) showGroupName()
        if (model.selected != previous) selectedGroupListeners.forEach { it() }
        grid.refresh()
    }

    override fun containerTick() {
        super.containerTick()
        grid.tick()
    }

    // ---- Drawing ----

    override fun renderBg(g: GuiGraphics, partialTick: Float, mx: Int, my: Int) {
        if (!configMode) {
            renderItemsPanel(g)
            renderCraftingPanel(g)
            renderInventoryPanel(g)
            renderActionPanel(g)
        } else {
            renderConfigPanel(g)
        }
        views.forEach { it.render(g, mx, my) }
    }

    private fun renderItemsPanel(g: GuiGraphics) {
        UiDraw.panelItems(g, layout.storage)
        UiDraw.inset(g, layout.search)
        UiDraw.icon(g, UiDraw.IconType.SEARCH, layout.search.x + layout.search.w - SEARCH_ICON_W + 4, layout.search.y + 7)
        UiDraw.fullnessBar(g, layout.fullnessBar, model.usedSlots, model.totalSlots)
    }

    private fun renderCraftingPanel(g: GuiGraphics) {
        UiDraw.panelCrafting(g, layout.crafting)
        for (i in 0 until 9) UiDraw.slot(g, layout.craftGrid.x + (i % 3) * Layout.CELL, layout.craftGrid.y + (i / 3) * Layout.CELL)
        UiDraw.slot(g, layout.resultSlot.x, layout.resultSlot.y)
        UiDraw.icon(g, UiDraw.IconType.RIGHT, layout.craftArrow)
    }

    private fun renderInventoryPanel(g: GuiGraphics) {
        UiDraw.panelInventory(g, layout.inventory)
        val invX = layout.inventory.x + Layout.PAD
        val invY = layout.inventory.y + Layout.PAD
        for (i in 0 until 27) UiDraw.slot(g, invX + (i % 9) * Layout.CELL, invY + (i / 9) * Layout.CELL)
        for (i in 0 until 9) UiDraw.slot(g, invX + i * Layout.CELL, invY + 3 * Layout.CELL + 4)
    }

    private fun renderActionPanel(g: GuiGraphics) {
        if (!menu.placed) return
        UiDraw.panelAction(g, layout.enterPanel)
    }

    private fun renderConfigPanel(g: GuiGraphics) {
        UiDraw.panelConfig(g, layout.config)
        UiDraw.inset(g, layout.nameField)
        UiDraw.fullnessBar(g, layout.fullnessBar, model.usedSlots, model.totalSlots)
    }

    override fun renderLabels(g: GuiGraphics, mx: Int, my: Int) {}

    override fun render(g: GuiGraphics, mx: Int, my: Int, partialTick: Float) {
        super.render(g, mx, my, partialTick)
        renderTooltip(g, mx, my)
    }

    override fun renderTooltip(g: GuiGraphics, x: Int, y: Int) {
        super.renderTooltip(g, x, y)
        if (hoveredSlot?.hasItem() == true && !configMode) return
        val lines = views.firstNotNullOfOrNull { it.tooltip(x, y) } ?: return
        g.renderTooltip(font, lines, Optional.empty(), x, y)
    }

    // ---- Input ----

    override fun mouseClicked(mx: Double, my: Double, button: Int): Boolean {
        if (views.any { it.mouseClicked(mx.toInt(), my.toInt(), button) }) {
            textBoxes.forEach { it.isFocused = false }
            return true
        }
        return super.mouseClicked(mx, my, button)
    }

    override fun mouseDragged(mx: Double, my: Double, button: Int, dx: Double, dy: Double): Boolean =
        views.any { it.mouseDragged(mx.toInt(), my.toInt()) } || super.mouseDragged(mx, my, button, dx, dy)

    override fun mouseReleased(mx: Double, my: Double, button: Int): Boolean {
        val handled = views.map { it.mouseReleased(mx.toInt(), my.toInt()) }.any { it }
        return handled || super.mouseReleased(mx, my, button)
    }

    override fun mouseScrolled(mx: Double, my: Double, scrollX: Double, scrollY: Double): Boolean {
        val delta = sign(scrollY).toInt()
        return views.any { it.mouseScrolled(mx.toInt(), my.toInt(), delta) } || super.mouseScrolled(mx, my, scrollX, scrollY)
    }

    override fun keyPressed(key: Int, scanCode: Int, modifiers: Int): Boolean {
        if (hasControlDown() && !configMode) {
            val group = key - InputConstants.KEY_1
            if (group in 0..8 && group < model.groups.size) { actions.selectGroup(group); return true }
        }
        val focused = textBoxes.firstOrNull { it.isFocused }
        if (focused != null) {
            if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) {
                focused.isFocused = false
                return true
            }
            if (key != InputConstants.KEY_ESCAPE) {
                focused.keyPressed(key, scanCode, modifiers)
                return true
            }
        } else if (ClientInit.storageKey.matches(key, scanCode)) {
            onClose()
            return true
        }
        return super.keyPressed(key, scanCode, modifiers)
    }

    private companion object {
        const val OFFSCREEN = -10000
        const val SEARCH_ICON_W = 14
    }
}
