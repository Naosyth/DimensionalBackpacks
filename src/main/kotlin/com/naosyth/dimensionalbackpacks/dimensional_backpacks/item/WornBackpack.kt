package com.naosyth.dimensionalbackpacks.dimensional_backpacks.item

import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.neoforged.fml.ModList

/** Finds the backpack an entity is wearing: the chest slot first, then a Curios slot if Curios is installed. */
object WornBackpack {
    fun find(entity: LivingEntity): ItemStack {
        val chest = entity.getItemBySlot(EquipmentSlot.CHEST)
        if (chest.item is BackpackItem) return chest
        if (ModList.get().isLoaded("curios")) return CuriosCompat.find(entity)
        return ItemStack.EMPTY
    }
}
