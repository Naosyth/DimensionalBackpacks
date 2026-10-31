package com.naosyth.dimensionalbackpacks.dimensional_backpacks.client.storage

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag

/** Drawing primitives in the vanilla container style. */
internal object UiDraw {
    private var PANEL_ITEMS_SPRITE = ResourceLocation.fromNamespaceAndPath("dimensional_backpacks", "storage/panel_items")
    private var PANEL_CRAFTING_SPRITE = ResourceLocation.fromNamespaceAndPath("dimensional_backpacks", "storage/panel_crafting")
    private var PANEL_INVENTORY_SPRITE = ResourceLocation.fromNamespaceAndPath("dimensional_backpacks", "storage/panel_inventory")
    private var PANEL_ACTION_SPRITE = ResourceLocation.fromNamespaceAndPath("dimensional_backpacks", "storage/panel_action")
    private var PANEL_CONFIG_SPRITE = ResourceLocation.fromNamespaceAndPath("dimensional_backpacks", "storage/panel_config")

    private var INSET_SPRITE = ResourceLocation.fromNamespaceAndPath("dimensional_backpacks", "storage/inset")

    private val SLOT_SPRITE = ResourceLocation.fromNamespaceAndPath("dimensional_backpacks", "storage/slot")

    private val SEARCH_TRACK_SPRITE = ResourceLocation.fromNamespaceAndPath("dimensional_backpacks", "storage/scroller_background")
    private val SEARCH_THUMB_SPRITE = ResourceLocation.fromNamespaceAndPath("dimensional_backpacks", "storage/scroller")

    private val BUTTON_SPRITE = ResourceLocation.fromNamespaceAndPath("dimensional_backpacks", "storage/button")
    private val BUTTON_HIGHLIGHTED_SPRITE = ResourceLocation.fromNamespaceAndPath("dimensional_backpacks", "storage/button_highlighted")
    private val BUTTON_DISABLED_SPRITE = ResourceLocation.fromNamespaceAndPath("dimensional_backpacks", "storage/button_disabled")

    private val STORAGE_ROW_SPRITE = ResourceLocation.fromNamespaceAndPath("dimensional_backpacks", "storage/storage_row")
    private val STORAGE_ROW_DISABLED_SPRITE = ResourceLocation.fromNamespaceAndPath("dimensional_backpacks", "storage/storage_row_disabled")

    enum class IconType {
        GRIP,
        PLUS,
        UP,
        DOWN,
        RIGHT,
        GEAR,
        SEARCH,
        TRASH,
        SORT_AZ,
        SORT_ZA,
        SORT_09,
        SORT_90
    }

    private val ICON_MAP = mapOf(
        IconType.GRIP to ResourceLocation.fromNamespaceAndPath("dimensional_backpacks", "storage/grip"),
        IconType.PLUS to ResourceLocation.fromNamespaceAndPath("dimensional_backpacks", "storage/plus"),
        IconType.UP to ResourceLocation.fromNamespaceAndPath("dimensional_backpacks", "storage/up"),
        IconType.DOWN to ResourceLocation.fromNamespaceAndPath("dimensional_backpacks", "storage/down"),
        IconType.RIGHT to ResourceLocation.fromNamespaceAndPath("dimensional_backpacks", "storage/right"),
        IconType.GEAR to ResourceLocation.fromNamespaceAndPath("dimensional_backpacks", "storage/gear"),
        IconType.SEARCH to ResourceLocation.fromNamespaceAndPath("dimensional_backpacks", "storage/search"),
        IconType.TRASH to ResourceLocation.fromNamespaceAndPath("dimensional_backpacks", "storage/trash"),
        IconType.SORT_AZ to ResourceLocation.fromNamespaceAndPath("dimensional_backpacks", "storage/sort_alpha_asc"),
        IconType.SORT_ZA to ResourceLocation.fromNamespaceAndPath("dimensional_backpacks", "storage/sort_alpha_desc"),
        IconType.SORT_09 to ResourceLocation.fromNamespaceAndPath("dimensional_backpacks", "storage/sort_number_asc"),
        IconType.SORT_90 to ResourceLocation.fromNamespaceAndPath("dimensional_backpacks", "storage/sort_number_desc")
    )

    const val ACCENT = 0xFFFFC107.toInt()
    const val TEXT = 0xFF404040.toInt()
    const val TEXT_LIGHT = 0xFFE0E0E0.toInt()

    val font get() = Minecraft.getInstance().font

    fun panelItems(g: GuiGraphics, r: Rect) {
        g.blitSprite(PANEL_ITEMS_SPRITE, r.x, r.y, r.w, r.h)
    }

    fun panelCrafting(g: GuiGraphics, r: Rect) {
        g.blitSprite(PANEL_CRAFTING_SPRITE, r.x, r.y, r.w, r.h)
    }

    fun panelInventory(g: GuiGraphics, r: Rect) {
        g.blitSprite(PANEL_INVENTORY_SPRITE, r.x, r.y, r.w, r.h)
    }

    fun panelAction(g: GuiGraphics, r: Rect) {
        g.blitSprite(PANEL_ACTION_SPRITE, r.x, r.y, r.w, r.h)
    }

    fun panelConfig(g: GuiGraphics, r: Rect) {
        g.blitSprite(PANEL_CONFIG_SPRITE, r.x, r.y, r.w, r.h)
    }

    fun inset(g: GuiGraphics, r: Rect) {
        g.blitSprite(INSET_SPRITE, r.x, r.y, r.w, r.h)
    }

    fun scrollTrack(g: GuiGraphics, r: Rect) {
        g.blitSprite(SEARCH_TRACK_SPRITE, r.x, r.y, r.w, r.h)
    }

    fun scrollThumb(g: GuiGraphics, r: Rect) {
        g.blitSprite(SEARCH_THUMB_SPRITE, r.x, r.y, r.w, r.h)
    }

    enum class ButtonVariant {
        NORMAL,
        HIGHLIGHTED,
        DISABLED
    }

    fun button(g: GuiGraphics, r: Rect, variant: ButtonVariant = ButtonVariant.NORMAL) {
        val sprite = when (variant) {
            ButtonVariant.NORMAL -> BUTTON_SPRITE
            ButtonVariant.HIGHLIGHTED -> BUTTON_HIGHLIGHTED_SPRITE
            ButtonVariant.DISABLED -> BUTTON_DISABLED_SPRITE
        }

        g.blitSprite(sprite, r.x, r.y, r.w, r.h)
    }

    fun storageRow(g: GuiGraphics, r: Rect, enabled: Boolean) {
        g.blitSprite(if (enabled) STORAGE_ROW_SPRITE else STORAGE_ROW_DISABLED_SPRITE, r.x, r.y, r.w, r.h)
        icon(g, IconType.GRIP, Rect(r.x, r.y + 2, 16, 16))
    }

    fun slot(g: GuiGraphics, x: Int, y: Int) {
        g.blitSprite(SLOT_SPRITE, x, y, 18, 18)
    }

    fun icon(g: GuiGraphics, icon: IconType, cx: Int, cy: Int, size: Int = 16) {
        val sprite = ICON_MAP[icon] ?: return
        g.blitSprite(sprite, cx - size / 2, cy - size / 2, size, size)
    }

    fun icon(g: GuiGraphics, icon: IconType, r: Rect, size: Int = 16) = icon(g, icon, r.centerX, r.centerY, size)

    fun text(g: GuiGraphics, text: String, x: Int, y: Int, color: Int = TEXT) {
        g.drawString(font, text, x, y, color, false)
    }

    fun centeredText(g: GuiGraphics, text: String, r: Rect, color: Int = TEXT) {
        g.drawString(font, text, r.centerX - font.width(text) / 2, r.centerY - 4, color, false)
    }

    /** A small number in the bottom-right corner of the 16x16 item at [x], [y], drawn above the item. */
    fun cornerText(g: GuiGraphics, text: String, x: Int, y: Int) {
        val scale = 0.65f
        g.pose().pushPose()
        g.pose().translate(x + 16f - font.width(text) * scale, y + 16f - 8 * scale, 200f)
        g.pose().scale(scale, scale, 1f)
        g.drawString(font, text, 0, 0, 0xFFFFFF, true)
        g.pose().popPose()
    }

    /** The same lines vanilla shows when hovering [stack]. */
    fun itemTooltip(stack: ItemStack): List<Component> {
        val mc = Minecraft.getInstance()
        val flag = if (mc.options.advancedItemTooltips) TooltipFlag.Default.ADVANCED else TooltipFlag.Default.NORMAL
        return stack.getTooltipLines(Item.TooltipContext.of(mc.level), mc.player, flag)
    }

    fun fullnessBar(g: GuiGraphics, r: Rect, used: Int, total: Int) {
        val fullness = if (total == 0) 0f else used.toFloat() / total
        g.blitSprite(SEARCH_TRACK_SPRITE, r.x, r.y, r.w, r.h)
        g.fill(r.x, r.y, r.right, r.bottom, 0xFF505050.toInt())
        val color = when {
            fullness > 0.9f -> 0xFFD05050.toInt()
            fullness > 0.6f -> 0xFFD0A030.toInt()
            else -> 0xFF50B050.toInt()
        }
        g.fill(r.x, r.y, r.x + (r.w * fullness).toInt(), r.bottom, color)
    }
}
