package com.naosyth.dimensionalbackpacks.dimensional_backpacks.item

import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.common.util.TriState
import top.theillusivec4.curios.api.CuriosApi
import top.theillusivec4.curios.api.event.CurioCanEquipEvent

/** Only touched when Curios is loaded, so the Curios classes are never resolved otherwise. */
object CuriosCompat {
    fun find(entity: LivingEntity): ItemStack =
        CuriosApi.getCuriosInventory(entity)
            .flatMap { it.findFirstCurio { stack -> stack.item is BackpackItem } }
            .map { it.stack() }
            .orElse(ItemStack.EMPTY)

    /** Vetoes equipping a backpack in a Curios slot while one is already worn on the chest. */
    fun onCanEquip(event: CurioCanEquipEvent) {
        if (event.stack.item !is BackpackItem) return
        if (event.slotContext.entity().getItemBySlot(EquipmentSlot.CHEST).item is BackpackItem) {
            event.equipResult = TriState.FALSE
        }
    }
}
