package com.naosyth.dimensionalbackpacks.dimensional_backpacks.item

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import top.theillusivec4.curios.api.CuriosApi

/** Only touched when Curios is loaded, so the Curios classes are never resolved otherwise. */
object CuriosCompat {
    fun find(entity: LivingEntity): ItemStack =
        CuriosApi.getCuriosInventory(entity)
            .flatMap { it.findFirstCurio { stack -> stack.item is BackpackItem } }
            .map { it.stack() }
            .orElse(ItemStack.EMPTY)
}
