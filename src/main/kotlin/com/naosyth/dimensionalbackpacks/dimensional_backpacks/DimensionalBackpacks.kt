package com.naosyth.dimensionalbackpacks.dimensional_backpacks

import com.naosyth.dimensionalbackpacks.dimensional_backpacks.block.ModBlockEntities
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.block.ModBlocks
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.item.ModComponents
import net.minecraft.world.item.CreativeModeTabs
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.command.DebugCommands
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.item.ModItems
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.BackpackEvents
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
        MOD_BUS.addListener<BuildCreativeModeTabContentsEvent> { event ->
            if (event.tabKey == CreativeModeTabs.TOOLS_AND_UTILITIES) {
                event.accept(ModItems.BACKPACK_TIER_1.get())
                event.accept(ModItems.BACKPACK_TIER_2.get())
                event.accept(ModItems.BACKPACK_TIER_3.get())
            }
        }
        NeoForge.EVENT_BUS.addListener<RegisterCommandsEvent> { DebugCommands.register(it.dispatcher) }
        NeoForge.EVENT_BUS.addListener(BackpackEvents::onPlayerTick)
        NeoForge.EVENT_BUS.addListener(BackpackEvents::onServerTick)
        NeoForge.EVENT_BUS.addListener(BackpackEvents::onServerStarted)
    }
}
