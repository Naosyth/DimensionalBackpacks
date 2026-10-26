package com.naosyth.dimensionalbackpacks.dimensional_backpacks.block

import com.mojang.datafixers.types.Type
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.DimensionalBackpacks
import net.minecraft.core.registries.Registries
import net.minecraft.world.level.block.entity.BlockEntityType
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.Optional

object ModBlockEntities {
    val REGISTRY: DeferredRegister<BlockEntityType<*>> =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, DimensionalBackpacks.ID)

    val BACKPACK = REGISTRY.register("backpack") { ->
        BlockEntityType.Builder.of(
            ::BackpackBlockEntity,
            *ModBlocks.BACKPACKS.values.map { it.get() }.toTypedArray()
        ).build(noDatafixerType())
    }

    // Modded block entities have no datafixer type. Returning the Java platform type keeps Kotlin from null-checking it.
    private fun noDatafixerType() = Optional.empty<Type<*>>().orElse(null)
}
