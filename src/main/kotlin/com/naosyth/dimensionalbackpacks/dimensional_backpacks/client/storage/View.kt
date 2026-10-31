package com.naosyth.dimensionalbackpacks.dimensional_backpacks.client.storage

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component

/** A rectangle in absolute screen coordinates. */
internal class Rect(val x: Int, val y: Int, val w: Int, val h: Int) {
    val right get() = x + w
    val bottom get() = y + h
    val centerX get() = x + w / 2
    val centerY get() = y + h / 2

    fun contains(px: Int, py: Int) = px >= x && px < right && py >= y && py < bottom
    fun shrink(by: Int) = Rect(x + by, y + by, w - 2 * by, h - 2 * by)
}

/**
 * A part of the storage screen that draws itself and handles its own input, all in absolute
 * screen coordinates. Event methods return true when they consumed the event.
 */
internal interface View {
    fun render(g: GuiGraphics, mx: Int, my: Int) {}
    fun tooltip(mx: Int, my: Int): List<Component>? = null
    fun mouseClicked(mx: Int, my: Int, button: Int): Boolean = false
    fun mouseDragged(mx: Int, my: Int): Boolean = false
    fun mouseReleased(mx: Int, my: Int): Boolean = false
    fun mouseScrolled(mx: Int, my: Int, delta: Int): Boolean = false
}
