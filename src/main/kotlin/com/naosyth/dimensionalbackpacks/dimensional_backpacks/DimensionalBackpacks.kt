package com.naosyth.dimensionalbackpacks.dimensional_backpacks

import com.naosyth.dimensionalbackpacks.dimensional_backpacks.block.ModBlockEntities
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.block.ModBlocks
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.item.ModComponents
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.item.BackpackUndyeRecipe
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.item.BackpackCopyRecipe
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.repository.Pack
import net.minecraft.server.packs.repository.PackSource
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.command.DebugCommands
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.item.ModItems
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.network.ModNetwork
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.client.ClientInit
import net.neoforged.api.distmarker.Dist
import net.neoforged.fml.loading.FMLEnvironment
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
import net.neoforged.neoforge.event.AddPackFindersEvent
import net.neoforged.neoforge.registries.DeferredRegister
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS
import java.util.function.Supplier

@Mod(DimensionalBackpacks.ID)
object DimensionalBackpacks {
    const val ID = "dimensional_backpacks"

    val LOGGER: Logger = LogManager.getLogger(ID)
    private val CREATIVE_TABS = DeferredRegister.create<CreativeModeTab>(Registries.CREATIVE_MODE_TAB, ID)

    init {
        ModBlocks.REGISTRY.register(MOD_BUS)
        ModItems.REGISTRY.register(MOD_BUS)
        ModBlockEntities.REGISTRY.register(MOD_BUS)
        ModComponents.REGISTRY.register(MOD_BUS)
        com.naosyth.dimensionalbackpacks.dimensional_backpacks.storage.ModMenus.REGISTRY.register(MOD_BUS)
        BackpackUndyeRecipe.REGISTRY.register(MOD_BUS)
        BackpackCopyRecipe.REGISTRY.register(MOD_BUS)
        CREATIVE_TABS.register(
            "main",
            Supplier {
                CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.dimensional_backpacks"))
                    .icon { ItemStack(ModItems.ZIPPER.get()) }
                    .displayItems { _, output ->
                        ModItems.allBackpacks().forEach(output::accept)
                        output.accept(ModItems.DECORATIVE_WALL.get())
                        output.accept(ModItems.ZIPPER.get())
                    }
                    .build()
            }
        )
        CREATIVE_TABS.register(MOD_BUS)
        MOD_BUS.addListener<AddPackFindersEvent> { event ->
            event.addPackFinders(
                ResourceLocation.fromNamespaceAndPath(ID, "resourcepacks/dbsimple"),
                PackType.CLIENT_RESOURCES,
                Component.translatable("resourcePack.dimensional_backpacks.dbsimple"),
                PackSource.BUILT_IN,
                false,
                Pack.Position.TOP
            )
        }
        MOD_BUS.addListener<RegisterPayloadHandlersEvent> { ModNetwork.register(it) }
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientInit.init()
            ModLoadingContext.get().activeContainer.registerConfig(ModConfig.Type.CLIENT, com.naosyth.dimensionalbackpacks.dimensional_backpacks.client.storage.StoragePrefs.SPEC, "dimensional_backpacks-client.toml")
        }
        NeoForge.EVENT_BUS.addListener<RegisterCommandsEvent> { DebugCommands.register(it.dispatcher) }
        NeoForge.EVENT_BUS.addListener(BackpackEvents::onPlayerTick)
        NeoForge.EVENT_BUS.addListener(BackpackEvents::onServerTick)
        NeoForge.EVENT_BUS.addListener(BackpackEvents::onServerStarted)
        NeoForge.EVENT_BUS.addListener(BackpackEvents::onPlayerLoggedIn)
        NeoForge.EVENT_BUS.addListener(BackpackEvents::onBlockPlaced)
        ModLoadingContext.get().activeContainer.registerConfig(ModConfig.Type.COMMON, com.naosyth.dimensionalbackpacks.dimensional_backpacks.config.ModConfig.SPEC)
        if (ModList.get().isLoaded("curios")) {
            NeoForge.EVENT_BUS.addListener<CurioCanEquipEvent> { CuriosCompat.onCanEquip(it) }
        }
    }
}
