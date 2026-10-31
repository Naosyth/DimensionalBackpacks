package com.naosyth.dimensionalbackpacks.dimensional_backpacks.storage

import com.naosyth.dimensionalbackpacks.dimensional_backpacks.DimensionalBackpacks
import net.minecraft.core.registries.Registries
import net.minecraft.world.inventory.MenuType
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension
import net.neoforged.neoforge.registries.DeferredRegister

object ModMenus {
    val REGISTRY: DeferredRegister<MenuType<*>> = DeferredRegister.create(Registries.MENU, DimensionalBackpacks.ID)

    val STORAGE = REGISTRY.register("storage") { ->
        IMenuTypeExtension.create { id, inventory, buf -> StorageMenu(id, inventory, buf.readVarInt(), buf.readBoolean()) }
    }
}
