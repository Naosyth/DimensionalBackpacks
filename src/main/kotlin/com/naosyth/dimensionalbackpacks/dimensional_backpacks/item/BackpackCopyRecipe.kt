package com.naosyth.dimensionalbackpacks.dimensional_backpacks.item

import com.naosyth.dimensionalbackpacks.dimensional_backpacks.DimensionalBackpacks
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.config.ModConfig
import net.minecraft.core.HolderLookup
import net.minecraft.core.NonNullList
import net.minecraft.core.registries.Registries
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.CraftingBookCategory
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.CustomRecipe
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer
import net.minecraft.world.level.Level
import net.neoforged.neoforge.registries.DeferredRegister

/**
 * A linked backpack plus an unlinked one of the same tier gives a second backpack linked to the same
 * plot. The linked backpack stays in the grid, so only the blank one is used up.
 */
class BackpackCopyRecipe(category: CraftingBookCategory) : CustomRecipe(category) {
    private class Match(val linked: Int, val blank: Int)

    private fun find(input: CraftingInput): Match? {
        if (!ModConfig.isCopyingEnabled()) return null
        val filled = (0 until input.size()).filter { !input.getItem(it).isEmpty }
        if (filled.size != 2) return null
        val (first, second) = filled.map { it to input.getItem(it) }
        if (first.second.item !is BackpackItem || first.second.item != second.second.item) return null
        val firstLinked = first.second.has(ModComponents.PLOT_ID.get())
        val secondLinked = second.second.has(ModComponents.PLOT_ID.get())
        return when {
            firstLinked && !secondLinked -> Match(first.first, second.first)
            secondLinked && !firstLinked -> Match(second.first, first.first)
            else -> null
        }
    }

    override fun matches(input: CraftingInput, level: Level): Boolean = find(input) != null

    override fun assemble(input: CraftingInput, registries: HolderLookup.Provider): ItemStack {
        val match = find(input) ?: return ItemStack.EMPTY
        return input.getItem(match.linked).copyWithCount(1)
    }

    override fun getRemainingItems(input: CraftingInput): NonNullList<ItemStack> {
        val remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY)
        find(input)?.let { remaining[it.linked] = input.getItem(it.linked).copyWithCount(1) }
        return remaining
    }

    override fun canCraftInDimensions(width: Int, height: Int): Boolean = width * height >= 2

    override fun getSerializer(): RecipeSerializer<*> = SERIALIZER.get()

    companion object {
        val REGISTRY: DeferredRegister<RecipeSerializer<*>> =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, DimensionalBackpacks.ID)

        val SERIALIZER = REGISTRY.register("backpack_copy") { ->
            SimpleCraftingRecipeSerializer(::BackpackCopyRecipe)
        }
    }
}
