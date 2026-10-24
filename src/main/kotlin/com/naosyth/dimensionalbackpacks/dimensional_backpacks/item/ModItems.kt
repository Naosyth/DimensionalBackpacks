package com.naosyth.dimensionalbackpacks.dimensional_backpacks.item

import com.naosyth.dimensionalbackpacks.dimensional_backpacks.DimensionalBackpacks
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.block.BackpackBlock
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.block.ModBlocks
import net.minecraft.world.item.Item
import java.util.function.Supplier
import net.neoforged.neoforge.registries.DeferredRegister

object ModItems {
    val REGISTRY = DeferredRegister.createItems(DimensionalBackpacks.ID)

    val PLOT_WALL = REGISTRY.registerSimpleBlockItem("plot_wall", ModBlocks.PLOT_WALL)
    val HOTSPOT = REGISTRY.register("hotspot", Supplier { HotspotItem(Item.Properties()) })
    val ZIPPER = REGISTRY.register("dimensional_zipper", Supplier { ZipperItem(Item.Properties().stacksTo(1)) })

    private fun backpack(name: String, block: Supplier<BackpackBlock>) =
        REGISTRY.register(name, Supplier { BackpackItem(block.get(), Item.Properties().stacksTo(1)) })

    val BACKPACK_TIER_1 = backpack("backpack_tier_1", ModBlocks.BACKPACK_TIER_1)
    val BACKPACK_TIER_2 = backpack("backpack_tier_2", ModBlocks.BACKPACK_TIER_2)
    val BACKPACK_TIER_3 = backpack("backpack_tier_3", ModBlocks.BACKPACK_TIER_3)
}
