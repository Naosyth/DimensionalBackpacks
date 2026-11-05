package com.naosyth.dimensionalbackpacks.dimensional_backpacks.client.storage

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents

/** A clickable button. */
internal class Button(
    private val area: Rect,
    private val icon: (GuiGraphics, Rect) -> Unit,
    private val tooltip: () -> List<Component>? = { null },
    private val active: () -> Boolean = { false },
    private val visible: () -> Boolean = { true },
    private val progress: () -> Float = { 0f },
    private val onRelease: () -> Unit = {},
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
        val filled = (area.h * progress()).toInt()
        if (filled > 0) g.fill(area.x, area.bottom - filled, area.right, area.bottom, PROGRESS_COLOR)
    }

    override fun tooltip(mx: Int, my: Int) = if (visible() && area.contains(mx, my)) tooltip() else null

    override fun mouseClicked(mx: Int, my: Int, button: Int): Boolean {
        if (button != 0 || !visible() || !area.contains(mx, my)) return false
        onClick()
        Minecraft.getInstance().soundManager.play(
            SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 0.1f)
        )
        return true
    }

    override fun mouseReleased(mx: Int, my: Int): Boolean {
        onRelease()
        return false
    }

    private companion object {
        const val PROGRESS_COLOR = 0x8855FF55.toInt()
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
