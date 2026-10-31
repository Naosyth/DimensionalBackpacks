package com.naosyth.dimensionalbackpacks.dimensional_backpacks.client.storage

import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.StorageGroup
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component

/**
 * A row of tabs, one per group, each showing the block of the group's top storage. The selected tab is
 * drawn active. While [canAdd] is true a "+" tab after the last group creates a new one.
 */
internal class GroupBar(
    private val area: Rect,
    private val model: StorageModel,
    private val actions: StorageActions,
    private val canAdd: () -> Boolean
) : View {

    private var tabs: List<Button> = emptyList()
    private var tabsForCount = -1

    /** Tabs are rebuilt only when the number of groups changes. */
    private fun currentTabs(): List<Button> {
        if (tabsForCount != model.groups.size) {
            tabs = model.groups.indices.map(::groupTab) + addTab(model.groups.size)
            tabsForCount = model.groups.size
        }
        return tabs
    }

    private fun tabArea(index: Int) = Rect(area.x + index * (TAB + GAP) - 1, area.y, TAB, TAB)

    private fun groupTab(index: Int) = Button(
        area = tabArea(index),
        icon = { g, r -> drawGroupIcon(g, r, index) },
        tooltip = { groupTooltip(index) },
        active = { model.selected == index },
        onClick = { actions.selectGroup(index) }
    )

    private fun addTab(index: Int) = Button(
        area = tabArea(index),
        icon = { g, r -> UiDraw.icon(g, UiDraw.IconType.PLUS, r) },
        tooltip = { listOf(Component.literal("New group")) },
        visible = { canAdd() && model.groups.size < StorageGroup.MAX },
        onClick = { actions.addGroup() }
    )

    private fun drawGroupIcon(g: GuiGraphics, r: Rect, index: Int) {
        val group = model.groups.getOrNull(index) ?: return
        if (group.icon.isEmpty) {
            UiDraw.centeredText(g, "${index + 1}", r, 0xFF707070.toInt())
            return
        }
        g.renderItem(group.icon, r.x + 2, r.y + 2)
        UiDraw.cornerText(g, "${index + 1}", r.x + 2, r.y + 2)
    }

    private fun groupTooltip(index: Int): List<Component> {
        val group = model.groups.getOrNull(index) ?: return emptyList()
        val storages = if (group.size == 1) "1 storage" else "${group.size} storages"
        return listOf(
            Component.literal(model.groupName(index)),
            Component.literal(storages).withStyle(ChatFormatting.GRAY),
            Component.literal("Slots: ${group.usedSlots}/${group.totalSlots}").withStyle(ChatFormatting.GRAY)
        )
    }

    override fun render(g: GuiGraphics, mx: Int, my: Int) = currentTabs().forEach { it.render(g, mx, my) }

    override fun tooltip(mx: Int, my: Int) = currentTabs().firstNotNullOfOrNull { it.tooltip(mx, my) }

    override fun mouseClicked(mx: Int, my: Int, button: Int) = currentTabs().any { it.mouseClicked(mx, my, button) }

    companion object {
        const val TAB = 20
        private const val GAP = 2
    }
}
