package com.naosyth.dimensionalbackpacks.dimensional_backpacks.item

import com.mojang.serialization.Codec
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.DimensionalBackpacks
import net.minecraft.core.registries.Registries
import net.minecraft.network.codec.ByteBufCodecs
import net.neoforged.neoforge.registries.DeferredRegister

object ModComponents {
    val REGISTRY: DeferredRegister.DataComponents = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, DimensionalBackpacks.ID)

    /** Identifies which plot (interior) a backpack item points to. Absent until first placed. */
    val PLOT_ID = REGISTRY.registerComponentType<Int>("plot_id") {
        it.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT)
    }
}
