package com.naosyth.dimensionalbackpacks.dimensional_backpacks

import com.naosyth.dimensionalbackpacks.dimensional_backpacks.block.ModBlockEntities
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.block.ModBlocks
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.item.ModComponents
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.item.BackpackUndyeRecipe
import net.minecraft.world.item.CreativeModeTabs
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.command.DebugCommands
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.item.ModItems
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.network.ModNetwork
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.client.ClientInit
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.HotspotSessions
import net.neoforged.api.distmarker.Dist
import net.neoforged.fml.loading.FMLEnvironment
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.BackpackEvents
import net.neoforged.fml.ModList
import net.neoforged.fml.ModLoadingContext
import net.neoforged.fml.config.ModConfig
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.item.CuriosCompat
import top.theillusivec4.curios.api.event.CurioCanEquipEvent
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.RegisterCommandsEvent
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS

@Mod(DimensionalBackpacks.ID)
object DimensionalBackpacks {
    const val ID = "dimensional_backpacks"

    val LOGGER: Logger = LogManager.getLogger(ID)

    init {
        ModBlocks.REGISTRY.register(MOD_BUS)
        ModItems.REGISTRY.register(MOD_BUS)
        ModBlockEntities.REGISTRY.register(MOD_BUS)
        ModComponents.REGISTRY.register(MOD_BUS)
        BackpackUndyeRecipe.REGISTRY.register(MOD_BUS)
        MOD_BUS.addListener<BuildCreativeModeTabContentsEvent> { event ->
            if (event.tabKey == CreativeModeTabs.TOOLS_AND_UTILITIES) {
                for (item in ModItems.allBackpacks()) event.accept(item)
                event.accept(ModItems.HOTSPOT.get())
                event.accept(ModItems.DECORATIVE_WALL.get())
                event.accept(ModItems.ZIPPER.get())
            }
        }
        MOD_BUS.addListener<RegisterPayloadHandlersEvent> { ModNetwork.register(it) }
        NeoForge.EVENT_BUS.addListener<PlayerEvent.PlayerLoggedOutEvent> { HotspotSessions.end(it.entity) }
        if (FMLEnvironment.dist == Dist.CLIENT) ClientInit.init()
        NeoForge.EVENT_BUS.addListener<RegisterCommandsEvent> { DebugCommands.register(it.dispatcher) }
        NeoForge.EVENT_BUS.addListener(BackpackEvents::onPlayerTick)
        NeoForge.EVENT_BUS.addListener(BackpackEvents::onServerTick)
        NeoForge.EVENT_BUS.addListener(BackpackEvents::onServerStarted)
        NeoForge.EVENT_BUS.addListener(BackpackEvents::onBlockPlaced)
        ModLoadingContext.get().activeContainer.registerConfig(ModConfig.Type.COMMON, com.naosyth.dimensionalbackpacks.dimensional_backpacks.config.ModConfig.SPEC)
        if (ModList.get().isLoaded("curios")) {
            NeoForge.EVENT_BUS.addListener<CurioCanEquipEvent> { CuriosCompat.onCanEquip(it) }
        }
    }
}
