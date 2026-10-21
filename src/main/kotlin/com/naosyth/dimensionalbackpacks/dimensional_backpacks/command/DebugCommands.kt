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
                    Commands.literal("enter")
                        .then(Commands.argument("id", IntegerArgumentType.integer(1)).executes {
                            enter(it, IntegerArgumentType.getInteger(it, "id"))
                        })
                )
        )
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
