package com.naosyth.dimensionalbackpacks.dimensional_backpacks.client.storage

import net.minecraft.client.gui.GuiGraphics
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * A view of fixed-height rows inside [content] that scrolls with the mouse wheel or a scrollbar in [track].
 * Subclasses say how many rows there are and draw and click what is inside.
 */
internal abstract class ScrollView(protected val content: Rect, private val track: Rect) : View {
    protected abstract val rowHeight: Int
    protected abstract val totalRows: Int

    protected val visibleRows get() = max(1, content.h / rowHeight)
    private val maxScroll get() = max(0, totalRows - visibleRows)

    /** Index of the first visible row. */
    protected var scroll = 0
    private var draggingThumb = false

    fun resetScroll() {
        scroll = 0
    }

    protected abstract fun renderContent(g: GuiGraphics, mx: Int, my: Int)

    protected open fun contentClicked(mx: Int, my: Int, button: Int): Boolean = false

    final override fun render(g: GuiGraphics, mx: Int, my: Int) {
        scroll = scroll.coerceIn(0, maxScroll)
        renderContent(g, mx, my)
        renderTrack(g)
    }

    private fun renderTrack(g: GuiGraphics) {
        UiDraw.scrollTrack(g, track)
        UiDraw.scrollThumb(g, thumb())
    }

    private fun thumb(): Rect {
        if (maxScroll == 0) return Rect(track.x, track.y, track.w - 1, track.h)
        val h = max(14, track.h * visibleRows / totalRows)
        return Rect(track.x, track.y + (track.h - h) * scroll / maxScroll, track.w - 1, h)
    }

    private fun dragThumbTo(my: Int) {
        val room = track.h - thumb().h
        if (room <= 0) return
        val fraction = ((my - track.y - thumb().h / 2.0) / room).coerceIn(0.0, 1.0)
        scroll = (fraction * maxScroll).roundToInt()
    }

    override fun mouseClicked(mx: Int, my: Int, button: Int): Boolean {
        if (button == 0 && track.contains(mx, my)) {
            draggingThumb = true
            dragThumbTo(my)
            return true
        }
        return content.contains(mx, my) && contentClicked(mx, my, button)
    }

    override fun mouseDragged(mx: Int, my: Int): Boolean {
        if (!draggingThumb) return false
        dragThumbTo(my)
        return true
    }

    override fun mouseReleased(mx: Int, my: Int): Boolean {
        if (!draggingThumb) return false
        draggingThumb = false
        return true
    }

    override fun mouseScrolled(mx: Int, my: Int, delta: Int): Boolean {
        if (!content.contains(mx, my) && !track.contains(mx, my)) return false
        scroll = (scroll - delta).coerceIn(0, maxScroll)
        return true
    }
}
