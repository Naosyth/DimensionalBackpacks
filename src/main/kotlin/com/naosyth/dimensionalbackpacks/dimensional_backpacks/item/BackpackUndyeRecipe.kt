package com.naosyth.dimensionalbackpacks.dimensional_backpacks.item

import com.naosyth.dimensionalbackpacks.dimensional_backpacks.DimensionalBackpacks
import net.minecraft.core.HolderLookup
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.Registries
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.CraftingBookCategory
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.CustomRecipe
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer
import net.minecraft.world.level.Level
import net.neoforged.neoforge.registries.DeferredRegister

/** A lone backpack in the crafting grid comes out undyed, keeping its plot link. */
class BackpackUndyeRecipe(category: CraftingBookCategory) : CustomRecipe(category) {
    override fun matches(input: CraftingInput, level: Level): Boolean {
        val stack = single(input) ?: return false
        return stack.item is BackpackItem && stack.has(DataComponents.DYED_COLOR)
    }

    override fun assemble(input: CraftingInput, registries: HolderLookup.Provider): ItemStack {
        val result = single(input)?.copy() ?: return ItemStack.EMPTY
        result.setCount(1)
        result.remove(DataComponents.DYED_COLOR)
        return result
    }

    override fun canCraftInDimensions(width: Int, height: Int): Boolean = width * height >= 1

    override fun getSerializer(): RecipeSerializer<*> = SERIALIZER.get()

    private fun single(input: CraftingInput): ItemStack? {
        val items = (0 until input.size()).map { input.getItem(it) }.filter { !it.isEmpty }
        return items.singleOrNull()
    }

    companion object {
        val REGISTRY: DeferredRegister<RecipeSerializer<*>> =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, DimensionalBackpacks.ID)

        val SERIALIZER = REGISTRY.register("backpack_undye") { ->
            SimpleCraftingRecipeSerializer(::BackpackUndyeRecipe)
        }
    }
}
