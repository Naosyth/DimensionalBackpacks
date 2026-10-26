package com.naosyth.dimensionalbackpacks.dimensional_backpacks.item

import com.naosyth.dimensionalbackpacks.dimensional_backpacks.DimensionalBackpacks
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.block.BackpackBlock
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.block.ModBlocks
import net.minecraft.world.item.Item
import net.neoforged.neoforge.registries.DeferredItem
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.Tier
import java.util.function.Supplier
import net.neoforged.neoforge.registries.DeferredRegister

object ModItems {
    val REGISTRY = DeferredRegister.createItems(DimensionalBackpacks.ID)

    val PLOT_WALL = REGISTRY.registerSimpleBlockItem("plot_wall", ModBlocks.PLOT_WALL)
    val DECORATIVE_WALL = REGISTRY.registerSimpleBlockItem("decorative_wall", ModBlocks.DECORATIVE_WALL)
    val HOTSPOT = REGISTRY.register("hotspot", Supplier { HotspotItem(Item.Properties()) })
    val ZIPPER = REGISTRY.register("dimensional_zipper", Supplier { ZipperItem(Item.Properties().stacksTo(1)) })

    val BACKPACKS: Map<Tier, DeferredItem<BackpackItem>> = Tier.entries.associateWith { tier ->
        REGISTRY.register(tier.registryName, Supplier {
            BackpackItem(ModBlocks.backpack(tier).get(), Item.Properties().stacksTo(1))
        })
    }

    fun backpack(tier: Tier): DeferredItem<BackpackItem> = BACKPACKS.getValue(tier)

    /** Every backpack item, for registrations that apply to all tiers. */
    fun allBackpacks(): Array<Item> = BACKPACKS.values.map { it.get() }.toTypedArray()
}
