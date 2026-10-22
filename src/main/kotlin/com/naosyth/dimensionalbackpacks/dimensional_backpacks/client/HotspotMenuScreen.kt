package com.naosyth.dimensionalbackpacks.dimensional_backpacks.client

import com.naosyth.dimensionalbackpacks.dimensional_backpacks.network.HotspotMenuPayload
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.network.HotspotSetPrimaryPayload
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.network.HotspotUsePayload
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.neoforged.neoforge.network.PacketDistributor
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** Radial picker shown while the hotspot key is held. Releasing the key uses the hovered hotspot. */
class HotspotMenuScreen(
    private val entries: List<HotspotMenuPayload.Entry>,
    private val key: KeyMapping
) : Screen(Component.literal("Hotspots")) {
    private var mouseX = 0
    private var mouseY = 0
    private var primarySlot = entries.firstOrNull { it.primary }?.slot

    // Setting a primary with right-click shouldn't also use a hotspot when the key is released.
    private var changedPrimary = false

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 1) {
            hovered()?.let {
                primarySlot = entries[it].slot
                changedPrimary = true
                PacketDistributor.sendToServer(HotspotSetPrimaryPayload(entries[it].slot))
            }
            return true
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun isPauseScreen() = false

    private fun angleOf(index: Int) = -PI / 2 + 2 * PI * index / entries.size

    private fun hovered(): Int? {
        val dx = (mouseX - width / 2).toDouble()
        val dy = (mouseY - height / 2).toDouble()
        if (hypot(dx, dy) < DEAD_ZONE) return null
        val mouseAngle = atan2(dy, dx)
        return entries.indices.minBy { i ->
            val diff = abs(mouseAngle - angleOf(i)) % (2 * PI)
            minOf(diff, 2 * PI - diff)
        }
    }

    override fun renderBackground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {}

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        this.mouseX = mouseX
        this.mouseY = mouseY
        val cx = width / 2
        val cy = height / 2
        val hover = hovered()
        graphics.fill(cx - 2, cy - 2, cx + 2, cy + 2, 0xFFAAAAAA.toInt())
        entries.forEachIndexed { i, entry ->
            val x = cx + (cos(angleOf(i)) * RADIUS).toInt()
            val y = cy + (sin(angleOf(i)) * RADIUS).toInt()
            val border = when {
                i == hover -> 0xFFFFFFFF.toInt()
                entry.slot == primarySlot -> 0xFFFFC107.toInt()
                else -> 0xFF555555.toInt()
            }
            graphics.fill(x - 14, y - 14, x + 14, y + 14, border)
            graphics.fill(x - 13, y - 13, x + 13, y + 13, if (i == hover) 0xFF444444.toInt() else 0xFF222222.toInt())
            if (entry.icon.isEmpty) {
                graphics.drawCenteredString(font, "-", x, y - 4, 0xFF888888.toInt())
            } else {
                graphics.renderItem(entry.icon, x - 8, y - 8)
            }
        }
        if (hover != null) {
            val entry = entries[hover]
            val name = if (entry.icon.isEmpty) Component.literal("Nothing here") else entry.icon.hoverName
            val label = if (entry.slot == primarySlot) Component.literal("").append(name).append(" (primary)") else name
            graphics.drawCenteredString(font, label, cx, cy + RADIUS + 28, 0xFFFFFFFF.toInt())
            graphics.drawCenteredString(font, "Right-click to set primary", cx, cy + RADIUS + 42, 0xFF888888.toInt())
        }
    }

    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (key.matches(keyCode, scanCode)) {
            if (!changedPrimary) hovered()?.let { PacketDistributor.sendToServer(HotspotUsePayload(entries[it].slot)) }
            onClose()
            return true
        }
        return super.keyReleased(keyCode, scanCode, modifiers)
    }

    private companion object {
        const val RADIUS = 50
        const val DEAD_ZONE = 16.0
    }
}
