package com.naosyth.dimensionalbackpacks.dimensional_backpacks.network

import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.PlotManager
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.PlotTravel
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.storage.StorageMenu
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.client.ClientInit
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.client.ClientPlotColors
import net.minecraft.server.level.ServerPlayer
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent

object ModNetwork {
    fun register(event: RegisterPayloadHandlersEvent) {
        val registrar = event.registrar("2")
        registrar.playToServer(ExitPayload.TYPE, ExitPayload.CODEC) { _, context ->
            context.enqueueWork {
                val player = context.player() as? ServerPlayer ?: return@enqueueWork
                if (player.level().dimension() == PlotManager.DIMENSION) PlotTravel.exit(player)
            }
        }
        registrar.playToServer(StorageOpenPayload.TYPE, StorageOpenPayload.CODEC) { _, context ->
            context.enqueueWork { (context.player() as? ServerPlayer)?.let { StorageMenu.openWorn(it) } }
        }
        registrar.playToServer(StorageClickPayload.TYPE, StorageClickPayload.CODEC) { payload, context ->
            context.enqueueWork {
                val menu = context.player().containerMenu as? StorageMenu ?: return@enqueueWork
                if (menu.containerId == payload.containerId) menu.click(payload.template, payload.button, payload.shift)
            }
        }
        registrar.playToServer(StorageGroupPayload.TYPE, StorageGroupPayload.CODEC) { payload, context ->
            context.enqueueWork {
                val menu = context.player().containerMenu as? StorageMenu ?: return@enqueueWork
                if (menu.containerId == payload.containerId) menu.groupAction(payload.action, payload.index, payload.name)
            }
        }
        registrar.playToServer(StorageDepositPayload.TYPE, StorageDepositPayload.CODEC) { payload, context ->
            context.enqueueWork {
                val menu = context.player().containerMenu as? StorageMenu ?: return@enqueueWork
                if (menu.containerId == payload.containerId) menu.deposit(payload.matchingOnly)
            }
        }
        registrar.playToServer(StorageEnterPayload.TYPE, StorageEnterPayload.CODEC) { payload, context ->
            context.enqueueWork {
                val menu = context.player().containerMenu as? StorageMenu ?: return@enqueueWork
                if (menu.containerId == payload.containerId) menu.setEnterHeld(payload.held)
            }
        }
        registrar.playToServer(StorageConfigPayload.TYPE, StorageConfigPayload.CODEC) { payload, context ->
            context.enqueueWork {
                val menu = context.player().containerMenu as? StorageMenu ?: return@enqueueWork
                if (menu.containerId == payload.containerId) menu.configure(payload.index, payload.action, payload.target)
            }
        }
        registrar.playToClient(StorageSyncPayload.TYPE, StorageSyncPayload.CODEC) { payload, context ->
            context.enqueueWork { ClientInit.onStorageSync(payload) }
        }
        registrar.playToClient(PlotColorPayload.TYPE, PlotColorPayload.CODEC) { payload, context ->
            context.enqueueWork { ClientPlotColors.set(payload.plotId, payload.color) }
        }
    }
}
