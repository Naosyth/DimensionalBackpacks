package com.naosyth.dimensionalbackpacks.dimensional_backpacks.block

import com.naosyth.dimensionalbackpacks.dimensional_backpacks.DimensionalBackpacks
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.Tier
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.material.PushReaction
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

object ModBlocks {
    val REGISTRY: DeferredRegister.Blocks = DeferredRegister.createBlocks(DimensionalBackpacks.ID)

    private fun unbreakable() = BlockBehaviour.Properties.of().strength(-1.0f, 3600000.0f).noLootTable().lightLevel { 15 }

    val PLOT_WALL = REGISTRY.registerSimpleBlock("plot_wall", unbreakable())

    // Hotspots live in the wall and are only ever removed through the hotspot's own logic.
    val HOTSPOT = REGISTRY.register("hotspot", Supplier { HotspotBlock(unbreakable()) })

    private fun backpackProperties() = BlockBehaviour.Properties.of()
        .strength(2.0f)
        .sound(SoundType.WOOL)
        .noOcclusion()
        .pushReaction(PushReaction.BLOCK)

    val BACKPACK_TIER_1 = REGISTRY.register("backpack_tier_1", Supplier { BackpackBlock(Tier.TIER_1, backpackProperties()) })
    val BACKPACK_TIER_2 = REGISTRY.register("backpack_tier_2", Supplier { BackpackBlock(Tier.TIER_2, backpackProperties()) })
    val BACKPACK_TIER_3 = REGISTRY.register("backpack_tier_3", Supplier { BackpackBlock(Tier.TIER_3, backpackProperties()) })
}
