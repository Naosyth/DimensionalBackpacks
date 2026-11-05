package com.naosyth.dimensionalbackpacks.dimensional_backpacks.client.storage

import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.tooltip.TooltipComponent
import net.minecraft.world.item.ItemStack

/**
 * Everything below a storage tooltip's title: a [subtitle] line, then a grid of the storage's first slots.
 * [stacks] ends at the last filled slot; [size] is the storage's real slot count.
 */
internal class ContentsTooltip(val subtitle: Component, val stacks: List<ItemStack>, val size: Int) : TooltipComponent

internal class ClientContentsTooltip(private val tooltip: ContentsTooltip) : ClientTooltipComponent {
    private val cells = minOf(tooltip.size, maxOf(1, (tooltip.stacks.size + COLS - 1) / COLS) * COLS)
    private val rows = (cells + COLS - 1) / COLS

    override fun getHeight() = LINE_H + rows * CELL + GAP

    override fun getWidth(font: Font) = maxOf(font.width(tooltip.subtitle), minOf(cells, COLS) * CELL)

    override fun renderImage(font: Font, x: Int, y: Int, g: GuiGraphics) {
        g.drawString(font, tooltip.subtitle, x, y, 0xFFFFFFFF.toInt())
        val gridY = y + LINE_H + GAP
        for (i in 0 until cells) {
            val cx = x + (i % COLS) * CELL
            val cy = gridY + (i / COLS) * CELL
            UiDraw.slot(g, cx, cy)
            val stack = tooltip.stacks.getOrNull(i) ?: continue
            g.renderItem(stack, cx + 1, cy + 1)
            g.renderItemDecorations(font, stack, cx + 1, cy + 1)
        }
    }

    private companion object {
        const val COLS = 9
        const val CELL = 18
        const val GAP = 4
        const val LINE_H = 10
    }
}
