package com.naosyth.dimensionalbackpacks.dimensional_backpacks.client.storage

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component

/** A clickable button. */
internal class Button(
    private val area: Rect,
    private val icon: (GuiGraphics, Rect) -> Unit,
    private val tooltip: () -> List<Component>? = { null },
    private val active: () -> Boolean = { false },
    private val visible: () -> Boolean = { true },
    private val onClick: () -> Unit
) : View {

    override fun render(g: GuiGraphics, mx: Int, my: Int) {
        if (!visible()) return
        if (active() || area.contains(mx, my)) {
            UiDraw.button(g, area, UiDraw.ButtonVariant.HIGHLIGHTED)
        } else {
            UiDraw.button(g, area, UiDraw.ButtonVariant.NORMAL)
        }
        icon(g, area)
    }

    override fun tooltip(mx: Int, my: Int) = if (visible() && area.contains(mx, my)) tooltip() else null

    override fun mouseClicked(mx: Int, my: Int, button: Int): Boolean {
        if (button != 0 || !visible() || !area.contains(mx, my)) return false
        onClick()
        return true
    }
}

internal class FullnessBar(
    private val area: Rect,
    private val used: () -> Int,
    private val total: () -> Int,
    private val tooltip: () -> List<Component>? = { null }
) : View {
    override fun render(g: GuiGraphics, mx: Int, my: Int) {
        UiDraw.fullnessBar(g, area, used(), total())
    }

    override fun tooltip(mx: Int, my: Int) = if (area.contains(mx, my)) tooltip() else null
}
