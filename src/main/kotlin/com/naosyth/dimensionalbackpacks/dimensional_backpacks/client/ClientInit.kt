package com.naosyth.dimensionalbackpacks.dimensional_backpacks.client

import com.mojang.blaze3d.platform.InputConstants
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.network.ExitPayload
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.network.HotspotMenuPayload
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.network.HotspotMenuRequestPayload
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.network.HotspotUsePayload
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.HotspotUse
import net.minecraft.client.KeyMapping
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.client.renderer.entity.player.PlayerRenderer
import net.minecraft.client.resources.PlayerSkin
import net.neoforged.neoforge.client.event.EntityRenderersEvent
import net.neoforged.fml.ModList
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import top.theillusivec4.curios.api.client.CuriosRendererRegistry
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.item.ModItems
import net.minecraft.client.Minecraft
import net.minecraft.util.FastColor
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.block.DecorativeWallBlock
import net.minecraft.world.item.component.DyedItemColor
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.block.BackpackBlockEntity
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.block.ModBlocks
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.network.PacketDistributor
import org.lwjgl.glfw.GLFW
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS

/** Client-only setup. Only touched from the client, so it may reference client classes. */
object ClientInit {
    /** Ticks the key must be held before the hotspot menu opens instead of using the primary hotspot. */
    private const val HOLD_TICKS = 6

    private val hotspotKey = KeyMapping(
        "key.dimensional_backpacks.hotspot",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_Z,
        "key.categories.dimensional_backpacks"
    )

    // Ticks the key has been held; 0 when idle.
    private var held = 0

    private val exitKey = KeyMapping(
        "key.dimensional_backpacks.exit",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_X,
        "key.categories.dimensional_backpacks"
    )

    fun init() {
        MOD_BUS.addListener<RegisterKeyMappingsEvent> {
            it.register(hotspotKey)
            it.register(exitKey)
        }
        MOD_BUS.addListener<EntityRenderersEvent.AddLayers> { event ->
            for (skin in listOf(PlayerSkin.Model.WIDE, PlayerSkin.Model.SLIM)) {
                val renderer = event.getSkin(skin) as? PlayerRenderer ?: continue
                renderer.addLayer(BackpackLayer(renderer))
            }
        }
        if (ModList.get().isLoaded("curios")) {
            MOD_BUS.addListener<FMLClientSetupEvent> {
                it.enqueueWork { registerCuriosRenderers() }
            }
        }
        MOD_BUS.addListener<RegisterColorHandlersEvent.Item> { event ->
            event.register(
                { stack, tintIndex ->
                    if (tintIndex == 0) FastColor.ARGB32.opaque(soften(DyedItemColor.getOrDefault(stack, DEFAULT_DYE))) else -1
                },
                *ModItems.allBackpacks(),
                ModItems.DECORATIVE_WALL.get()
            )
        }
        MOD_BUS.addListener<RegisterColorHandlersEvent.Block> { event ->
            event.register(
                { _, level, pos, tintIndex ->
                    val color = (pos?.let { level?.getBlockEntity(it) } as? BackpackBlockEntity)?.color
                    if (tintIndex == 0) FastColor.ARGB32.opaque(soften(color ?: DEFAULT_DYE)) else -1
                },
                *ModBlocks.BACKPACKS.values.map { it.get() }.toTypedArray()
            )
            event.register(
                { _, _, pos, tintIndex ->
                    if (tintIndex == 0) FastColor.ARGB32.opaque(soften(ClientPlotColors.tint(pos))) else -1
                },
                ModBlocks.PLOT_WALL.get()
            )
            event.register(
                { state, _, _, tintIndex ->
                    if (tintIndex == 0) FastColor.ARGB32.opaque(soften(DecorativeWallBlock.rgb(state))) else -1
                },
                ModBlocks.DECORATIVE_WALL.get()
            )
        }
        NeoForge.EVENT_BUS.addListener<ClientTickEvent.Post> { tick() }
    }

    /** Colour of an undyed backpack. White leaves the model's textures exactly as painted. */
    const val DEFAULT_DYE = 0xFFFFFF

    /** How much of the dye colour shows through; 1.0 is the full dye, lower values wash it toward white. */
    private const val DYE_STRENGTH = 0.80f

    private fun soften(rgb: Int): Int {
        fun channel(shift: Int): Int {
            val c = (rgb shr shift) and 0xFF
            return (255 + (c - 255) * DYE_STRENGTH).toInt().coerceIn(0, 255)
        }
        return (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }

    // Kept in its own function so Curios classes are only resolved when Curios is present.
    private fun registerCuriosRenderers() {
        for (item in ModItems.BACKPACKS.values) {
            CuriosRendererRegistry.register(item.get()) { CuriosBackpackRenderer() }
        }
    }

    private fun tick() {
        val mc = Minecraft.getInstance()
        if (mc.player == null || mc.screen != null) {
            held = 0
            return
        }
        // The server ignores this outside a backpack.
        while (exitKey.consumeClick()) PacketDistributor.sendToServer(ExitPayload)
        if (hotspotKey.isDown) {
            held++
            if (held == HOLD_TICKS) PacketDistributor.sendToServer(HotspotMenuRequestPayload)
        } else {
            // A release before the hold threshold is a tap.
            if (held in 1 until HOLD_TICKS) PacketDistributor.sendToServer(HotspotUsePayload(HotspotUse.PRIMARY))
            held = 0
        }
    }

    fun openHotspotMenu(entries: List<HotspotMenuPayload.Entry>) {
        val mc = Minecraft.getInstance()
        if (mc.screen != null || entries.isEmpty()) return
        // The key was already released while the server was answering; nothing to pick.
        if (!InputConstants.isKeyDown(mc.window.window, hotspotKey.key.value)) return
        mc.setScreen(HotspotMenuScreen(entries, hotspotKey))
    }
}
