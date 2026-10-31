package com.naosyth.dimensionalbackpacks.dimensional_backpacks.client.storage

import com.naosyth.dimensionalbackpacks.dimensional_backpacks.network.StorageConfigPayload
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.network.StorageSyncPayload.Info
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The selected group's storages in priority order. Each row has an on/off switch for membership in the
 * group and arrows; rows can also be dragged to a new position.
 */
internal class StorageList(
    content: Rect,
    track: Rect,
    private val model: StorageModel,
    private val actions: StorageActions
) : ScrollView(content, track) {

    override val rowHeight = ROW_H
    override val totalRows get() = model.storages.size

    private var pressedIndex = -1
    private var pressY = 0
    private var dragging = false
    private var lastAutoScroll = 0L

    private val storages get() = model.storages

    private fun change(index: Int, action: Int, target: Int = 0) {
        actions.config(index, action, target)
        model.applyConfig(index, action, target)
    }

    // ---- Drawing ----

    override fun renderContent(g: GuiGraphics, mx: Int, my: Int) {
        UiDraw.inset(g, content)
        if (storages.isEmpty()) {
            UiDraw.centeredText(g, "No storage found", content, UiDraw.TEXT_LIGHT)
            return
        }
        if (dragging) autoScroll(my)
        for (visible in 0 until visibleRows) {
            val index = visible + scroll
            if (index >= storages.size) break
            val row = rowArea(visible)
            if (dragging && index == pressedIndex) g.fill(row.x, row.y, row.right, row.bottom, 0x40000000)
            else drawRow(g, storages[index], index, row, mx, my)
        }
        if (dragging) renderDrag(g, mx, my)
    }

    private fun rowArea(visibleIndex: Int) =
        Rect(content.x + 1, content.y + visibleIndex * ROW_H + 1, content.w - 2, ROW_H - 2)

    private fun renderDrag(g: GuiGraphics, mx:Int, my: Int) {
        val visible = targetIndex(my) - scroll
        if (visible in 0 until visibleRows) {
            val y = content.y + visible * ROW_H + if (targetIndex(my) > pressedIndex) ROW_H - 1 else 0
            g.fill(content.x + 1, y - 1, content.right - 1, y + 1, UiDraw.ACCENT)
        }
        g.pose().pushPose()
        g.pose().translate(0f, 0f, 300f)
        val floating = rowArea(0)
        drawRow(g, storages[pressedIndex], pressedIndex, Rect(floating.x, my - ROW_H / 2, floating.w, floating.h), mx, my)
        g.pose().popPose()
    }

    private fun drawRow(g: GuiGraphics, info: Info, index: Int, row: Rect, mx: Int, my: Int) {
        val variant = when {
            row.contains(mx, my) -> UiDraw.StorageRowVariant.HOVER
            info.enabled -> UiDraw.StorageRowVariant.NORMAL
            else -> UiDraw.StorageRowVariant.DISABLED
        }
        UiDraw.storageRow(g, row, variant)
        g.renderItem(info.icon, row.x + 13, row.y + 2)
        val nameColor = if (info.enabled) 0xFF202020.toInt() else 0xFF606060.toInt()
        val name = UiDraw.font.plainSubstrByWidth(info.icon.hoverName.string, row.w - 88)
        UiDraw.text(g, name, row.x + 33, row.y + 3, nameColor)
        drawFullness(g, info, row)
    }

    private fun drawFullness(g: GuiGraphics, info: Info, row: Rect) {
        val slotsText = "${info.used}/${info.size} slots"
        UiDraw.text(g, slotsText, row.x + 33, row.y + 12, 0xFF505050.toInt())
        val barX = row.x + 33 + UiDraw.font.width(slotsText) + 4
        val barW = row.w - 32 - (barX - row.x) - 4
        if (barW <= 6) return
        val fullness = if (info.size == 0) 0f else info.used.toFloat() / info.size
        val color = when {
            fullness > 0.9f -> 0xFFD05050.toInt()
            fullness > 0.6f -> 0xFFD0A030.toInt()
            else -> 0xFF50B050.toInt()
        }
        g.fill(barX, row.y + 14, barX + barW, row.y + 17, FULLNESS_TRACK)
        g.fill(barX, row.y + 14, barX + (barW * fullness).roundToInt(), row.y + 17, color)
    }

    // ---- Input ----

    private fun indexAt(my: Int): Int? {
        if (my < content.y || my >= content.bottom) return null
        val visible = (my - content.y) / ROW_H
        return (visible + scroll).takeIf { visible < visibleRows && it < storages.size }
    }

    private fun targetIndex(my: Int) = (Math.floorDiv(my - content.y, ROW_H) + scroll).coerceIn(0, storages.size - 1)

    private fun autoScroll(my: Int) {
        val now = System.currentTimeMillis()
        if (now - lastAutoScroll < AUTO_SCROLL_MS) return
        when {
            my < content.y + EDGE && scroll > 0 -> scroll--
            my > content.bottom - EDGE -> scroll++
            else -> return
        }
        lastAutoScroll = now
    }

    override fun contentClicked(mx: Int, my: Int, button: Int): Boolean {
        if (button != 0) return false
        val index = indexAt(my) ?: return true

        // Record the press state; we determine whether it's a click or drag on release
        pressedIndex = index
        pressY = my
        dragging = false
        return true
    }

    override fun mouseDragged(mx: Int, my: Int): Boolean {
        if (pressedIndex < 0) return super.mouseDragged(mx, my)
        if (abs(my - pressY) > DRAG_THRESHOLD) {
            dragging = true
        }
        return true
    }

    override fun mouseReleased(mx: Int, my: Int): Boolean {
        if (pressedIndex < 0) return super.mouseReleased(mx, my)

        val target = targetIndex(my)
        if (dragging) {
            // If dragged to a different position, perform the reorder move
            if (target != pressedIndex) {
                change(pressedIndex, StorageConfigPayload.MOVE, target)
            }
        } else {
            // If it was just a clean click without dragging, toggle anywhere on the row
            change(pressedIndex, StorageConfigPayload.TOGGLE)
        }

        pressedIndex = -1
        dragging = false
        return true
    }

    override fun tooltip(mx: Int, my: Int): List<Component>? {
        if (dragging || !content.contains(mx, my)) return null
        val index = indexAt(my) ?: return null
        val info = storages[index]
        return listOf(
            info.icon.hoverName,
            Component.literal("Priority ${index + 1}").withStyle(ChatFormatting.GRAY),
            Component.literal("At ${info.pos.x}, ${info.pos.y}, ${info.pos.z} in the backpack").withStyle(ChatFormatting.DARK_GRAY),
            Component.literal("Drag to reorder").withStyle(ChatFormatting.DARK_GRAY)
        )
    }

    private companion object {
        const val ROW_H = 24
        const val DRAG_THRESHOLD = 3
        const val EDGE = 8
        const val AUTO_SCROLL_MS = 120
        const val FULLNESS_TRACK = 0xFF555555.toInt()
    }
}
