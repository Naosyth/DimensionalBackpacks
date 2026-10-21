package com.naosyth.dimensionalbackpacks.dimensional_backpacks.item

import com.naosyth.dimensionalbackpacks.dimensional_backpacks.block.BackpackBlock
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.PlotManager
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.Tier
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.EquipmentSlot
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

    // Right-clicking in the air wears the backpack; right-clicking a block places it.
    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> =
        swapWithEquipmentSlot(this, level, player, hand)

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
