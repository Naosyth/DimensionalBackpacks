package com.naosyth.dimensionalbackpacks.dimensional_backpacks.block

import com.naosyth.dimensionalbackpacks.dimensional_backpacks.DimensionalBackpacks
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.Tier
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.material.PushReaction
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.DeferredBlock
import java.util.function.Supplier

object ModBlocks {
    val REGISTRY: DeferredRegister.Blocks = DeferredRegister.createBlocks(DimensionalBackpacks.ID)

    private fun unbreakable() = BlockBehaviour.Properties.of().strength(-1.0f, 3600000.0f).isRedstoneConductor { _, _, _ -> false }.noLootTable().lightLevel { 15 }

    val PLOT_WALL = REGISTRY.register("plot_wall", Supplier { PlotWallBlock(unbreakable()) })

    // Breakable look-alike for building inside a backpack.
    val DECORATIVE_WALL = REGISTRY.register("decorative_wall", Supplier {
        DecorativeWallBlock(BlockBehaviour.Properties.of().strength(1.5f, 6.0f).sound(SoundType.WOOL))
    })

    private fun backpackProperties() = BlockBehaviour.Properties.of()
        .strength(0.0f)
        .sound(SoundType.WOOL)
        .noOcclusion()
        .pushReaction(PushReaction.BLOCK)

    val BACKPACKS: Map<Tier, DeferredBlock<BackpackBlock>> = Tier.entries.associateWith { tier ->
        REGISTRY.register(tier.registryName, Supplier { BackpackBlock(tier, backpackProperties()) })
    }

    fun backpack(tier: Tier): DeferredBlock<BackpackBlock> = BACKPACKS.getValue(tier)
}
