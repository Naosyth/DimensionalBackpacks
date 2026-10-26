package com.naosyth.dimensionalbackpacks.dimensional_backpacks.command

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.Plot
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.PlotLoader
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.PlotManager
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.PlotTravel
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.Tier
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.item.ModComponents
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.item.ModItems

/** Temporary development command for exercising plots before items/blocks exist. */
object DebugCommands {
    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal("dbp").requires { it.hasPermission(2) }
                .then(Commands.literal("new").executes { enter(it, null) })
                .then(Commands.literal("loaded").executes {
                    it.source.sendSuccess({ Component.literal("Active plots: ${PlotLoader.activePlotIds().sorted()}") }, false)
                    1
                })
                .then(
                    Commands.literal("backpack")
                        .then(Commands.argument("id", IntegerArgumentType.integer(1)).executes {
                            giveBackpack(it, IntegerArgumentType.getInteger(it, "id"))
                        })
                )
                .then(
                    Commands.literal("enter")
                        .then(Commands.argument("id", IntegerArgumentType.integer(1)).executes {
                            enter(it, IntegerArgumentType.getInteger(it, "id"))
                        })
                )
        )
    }

    /** Recovers access to a plot by giving a backpack of the plot's tier bound to it. */
    private fun giveBackpack(ctx: CommandContext<CommandSourceStack>, id: Int): Int {
        val player = ctx.source.playerOrException
        val pocket = PlotManager.getPocketLevel(ctx.source.server)
        val plot = pocket?.let { PlotManager.get(it).get(id) }
        if (plot == null) {
            ctx.source.sendFailure(Component.literal("No plot with id $id"))
            return 0
        }
        val stack = ItemStack(ModItems.backpack(plot.tier).get())
        stack.set(ModComponents.PLOT_ID.get(), id)
        if (!player.inventory.add(stack)) player.drop(stack, false)
        ctx.source.sendSuccess({ Component.literal("Gave a tier ${plot.tier.ordinal + 1} backpack for plot $id") }, true)
        return 1
    }

    private fun enter(ctx: CommandContext<CommandSourceStack>, id: Int?): Int {
        val player = ctx.source.playerOrException
        val pocket = PlotManager.getPocketLevel(ctx.source.server)
        if (pocket == null) {
            ctx.source.sendFailure(Component.literal("Pocket dimension is not loaded"))
            return 0
        }
        val manager = PlotManager.get(pocket)
        val plot: Plot
        if (id == null) {
            plot = PlotManager.allocate(ctx.source.server, Tier.TIER_1)!!
        } else {
            plot = manager.get(id) ?: run {
                ctx.source.sendFailure(Component.literal("No plot with id $id"))
                return 0
            }
        }
        PlotTravel.enter(player, plot, pocket)
        ctx.source.sendSuccess({ Component.literal("Entered plot ${plot.id}") }, false)
        return 1
    }
}
