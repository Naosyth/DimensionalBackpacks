package com.naosyth.dimensionalbackpacks.dimensional_backpacks.client

import com.mojang.blaze3d.platform.InputConstants
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

    fun init() {
        MOD_BUS.addListener<RegisterKeyMappingsEvent> { it.register(hotspotKey) }
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
        NeoForge.EVENT_BUS.addListener<ClientTickEvent.Post> { tick() }
    }

    // Kept in its own function so Curios classes are only resolved when Curios is present.
    private fun registerCuriosRenderers() {
        for (item in listOf(ModItems.BACKPACK_TIER_1, ModItems.BACKPACK_TIER_2, ModItems.BACKPACK_TIER_3)) {
            CuriosRendererRegistry.register(item.get()) { CuriosBackpackRenderer() }
        }
    }

    private fun tick() {
        val mc = Minecraft.getInstance()
        if (mc.player == null || mc.screen != null) {
            held = 0
            return
        }
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
