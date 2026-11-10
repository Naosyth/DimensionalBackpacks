package com.naosyth.dimensionalbackpacks.dimensional_backpacks.client.compat

import com.naosyth.dimensionalbackpacks.dimensional_backpacks.DimensionalBackpacks
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.client.storage.StorageScreen
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.network.StorageRecipePayload
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.storage.ModMenus
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.storage.StorageMenu
import mezz.jei.api.IModPlugin
import mezz.jei.api.JeiPlugin
import mezz.jei.api.constants.RecipeTypes
import mezz.jei.api.gui.ingredient.IRecipeSlotsView
import mezz.jei.api.recipe.RecipeIngredientRole
import mezz.jei.api.recipe.RecipeType
import mezz.jei.api.recipe.transfer.IRecipeTransferContext
import mezz.jei.api.recipe.transfer.IRecipeTransferError
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper
import mezz.jei.api.recipe.transfer.RecipeTransferResult
import mezz.jei.api.registration.IRecipeTransferRegistration
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.CraftingRecipe
import net.minecraft.world.item.crafting.RecipeHolder
import net.neoforged.neoforge.network.PacketDistributor
import java.util.Optional

@JeiPlugin
class JeiPlugin : IModPlugin {
    override fun getPluginUid(): ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(DimensionalBackpacks.ID, "jei")

    override fun registerRecipeTransferHandlers(registration: IRecipeTransferRegistration) {
        registration.addRecipeTransferHandler(StorageRecipeTransfer(registration.transferHelper), RecipeTypes.CRAFTING)
    }
}

private class StorageRecipeTransfer(private val helper: IRecipeTransferHandlerHelper) :
    IRecipeTransferHandler<StorageMenu, RecipeHolder<CraftingRecipe>> {

    override fun getContainerClass(): Class<out StorageMenu> = StorageMenu::class.java
    override fun getMenuType(): Optional<MenuType<StorageMenu>> = Optional.of(ModMenus.STORAGE.get())
    override fun getRecipeType(): RecipeType<RecipeHolder<CraftingRecipe>> = RecipeTypes.CRAFTING

    override fun transferRecipe(
        context: IRecipeTransferContext<RecipeHolder<CraftingRecipe>, StorageMenu>,
        doTransfer: Boolean
    ): IRecipeTransferError? {
        val error = transfer(context.container, context.recipe, context.recipeSlots, context.player,
            context.screen as? StorageScreen, context.isMaxTransfer, doTransfer)
        if (doTransfer && error == null) context.completeRecipeTransfer(RecipeTransferResult.SUCCESS)
        return error
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun transferRecipe(
        container: StorageMenu,
        recipe: RecipeHolder<CraftingRecipe>,
        recipeSlots: IRecipeSlotsView,
        player: Player,
        maxTransfer: Boolean,
        doTransfer: Boolean
    ): IRecipeTransferError? =
        transfer(container, recipe, recipeSlots, player, Minecraft.getInstance().screen as? StorageScreen, maxTransfer, doTransfer)

    private fun transfer(
        container: StorageMenu,
        recipe: RecipeHolder<CraftingRecipe>,
        recipeSlots: IRecipeSlotsView,
        player: Player,
        screen: StorageScreen?,
        maxTransfer: Boolean,
        doTransfer: Boolean
    ): IRecipeTransferError? {
        if (screen == null || !screen.recipeTransferReady) {
            return helper.createUserErrorWithTooltip(Component.literal("Open the backpack's items view first."))
        }
        val slots = recipeSlots.getSlotViews(RecipeIngredientRole.INPUT)
        if (slots.size != 9 || slots.any { !it.isEmpty && it.itemStacks.findAny().isEmpty }) {
            return helper.createUserErrorWithTooltip(Component.literal("This recipe cannot be placed in the crafting grid."))
        }
        val options = slots.map { it.itemStacks.filter { stack -> !stack.isEmpty }.toList() }
        val ingredients = helper.getGuiSlotIndexToIngredientMap(recipe)
        val available = screen.recipeTransferItems.map { Available(it.stack, it.count.coerceAtMost(9).toInt()) } +
            player.inventory.items.filter { !it.isEmpty }.map { Available(it, it.count.coerceAtMost(9)) } +
            (StorageMenu.CRAFT_START until StorageMenu.INVENTORY_START).map { container.slots[it].item }
                .filter { !it.isEmpty }.map { Available(it, it.count.coerceAtMost(9)) }

        val candidates = options.mapIndexed { slot, choices ->
            available.indices.filter { index ->
                val item = available[index]
                val ingredient = ingredients[slot]
                choices.any { option ->
                    if (ingredient != null) option.item == item.stack.item && ingredient.test(item.stack)
                    else ItemStack.isSameItemSameComponents(option, item.stack)
                }
            }
        }
        val assigned = RecipeAssignment.assign(available.map { it.count }, candidates)
        val missing = slots.indices.filter { options[it].isNotEmpty() && assigned[it] < 0 }.map { slots[it] }
        if (missing.isNotEmpty()) {
            return helper.createUserErrorForMissingSlots(Component.literal("Not enough ingredients."), missing)
        }
        if (!doTransfer) return null
        val templates = options.mapIndexed { slot, choices ->
            val selected = assigned[slot].takeIf { it >= 0 }?.let { available[it].stack }
                ?: slots[slot].displayedItemStack.orElse(null)
                ?: choices.firstOrNull()
            selected?.copyWithCount(1) ?: ItemStack.EMPTY
        }
        PacketDistributor.sendToServer(StorageRecipePayload(container.containerId, templates, maxTransfer))
        return null
    }

    private data class Available(val stack: ItemStack, val count: Int)
}
