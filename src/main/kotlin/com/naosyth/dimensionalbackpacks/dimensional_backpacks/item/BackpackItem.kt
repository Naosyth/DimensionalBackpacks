package com.naosyth.dimensionalbackpacks.dimensional_backpacks.item

import com.naosyth.dimensionalbackpacks.dimensional_backpacks.block.BackpackBlock
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.PlotManager
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.Tier
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.storage.StorageMenu
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.EquipmentSlot
import net.neoforged.fml.ModList
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Equipable
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState

class BackpackItem(block: BackpackBlock, properties: Properties) : BlockItem(block, properties), Equipable {
    val tier: Tier = block.tier

    override fun getEquipmentSlot(): EquipmentSlot = EquipmentSlot.CHEST

    // Only one backpack can be worn at a time, so a chest-slot backpack is refused while a Curios one is worn.
    override fun canEquip(stack: ItemStack, armorType: EquipmentSlot, entity: LivingEntity): Boolean =
        !hasCuriosBackpack(entity) && super.canEquip(stack, armorType, entity)

    private fun hasCuriosBackpack(entity: LivingEntity): Boolean =
        ModList.get().isLoaded("curios") && !CuriosCompat.find(entity).isEmpty

    // Sneak-use in the air still equips the backpack; ordinary air use opens its storage.
    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(hand)
        if (player.isShiftKeyDown) {
            if (hasCuriosBackpack(player)) {
                player.displayClientMessage(Component.literal("You can only wear one backpack at a time"), true)
                return InteractionResultHolder.fail(stack)
            }
            return swapWithEquipmentSlot(this, level, player, hand)
        }
        if (player is ServerPlayer && !StorageMenu.openHeld(player, hand)) return InteractionResultHolder.fail(stack)
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
    }

    // Backpacks may be carried inside a backpack, but never placed inside one.
    override fun canPlace(context: BlockPlaceContext, state: BlockState): Boolean {
        if (context.level.dimension() == PlotManager.DIMENSION) {
            context.player?.displayClientMessage(Component.literal("You can't place a backpack inside a backpack"), true)
            return false
        }
        return super.canPlace(context, state)
    }

    override fun appendHoverText(
        stack: ItemStack, context: Item.TooltipContext, tooltip: MutableList<Component>, flag: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltip, flag)
        val id = stack.get(ModComponents.PLOT_ID.get())
        if (id != null) tooltip.add(Component.literal("Plot #$id"))
    }
}
