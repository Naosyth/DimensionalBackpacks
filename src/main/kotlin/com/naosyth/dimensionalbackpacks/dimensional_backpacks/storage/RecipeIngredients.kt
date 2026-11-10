package com.naosyth.dimensionalbackpacks.dimensional_backpacks.storage

import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.items.IItemHandler

internal class RecipeIngredient(val template: ItemStack) {
    val slots = ArrayList<Int>()
    var remaining = 0
}

internal fun reserveStorageIngredients(
    handlers: List<IItemHandler>,
    groups: List<RecipeIngredient>,
    reserved: MutableList<ItemStack>
): Boolean {
    for (handler in handlers) for (slot in 0 until handler.slots) {
        var stack = handler.getStackInSlot(slot)
        val group = groups.firstOrNull { it.remaining > 0 && ItemStack.isSameItemSameComponents(it.template, stack) }
            ?: continue
        while (group.remaining > 0 && ItemStack.isSameItemSameComponents(group.template, stack)) {
            val amount = minOf(group.remaining, stack.maxStackSize)
            val taken = handler.extractItem(slot, amount, false)
            if (taken.isEmpty) break
            reserved.add(taken)
            if (!ItemStack.isSameItemSameComponents(taken, group.template) || taken.count > amount) return false
            group.remaining -= taken.count
            stack = handler.getStackInSlot(slot)
        }
    }
    return true
}
