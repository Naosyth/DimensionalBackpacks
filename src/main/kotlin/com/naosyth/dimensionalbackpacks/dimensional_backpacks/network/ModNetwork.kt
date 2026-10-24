package com.naosyth.dimensionalbackpacks.dimensional_backpacks.network

import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.HotspotUse
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.PlotManager
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.PlotTravel
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.client.ClientInit
import net.minecraft.server.level.ServerPlayer
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent

object ModNetwork {
    fun register(event: RegisterPayloadHandlersEvent) {
        val registrar = event.registrar("1")
        registrar.playToServer(HotspotUsePayload.TYPE, HotspotUsePayload.CODEC) { payload, context ->
            context.enqueueWork {
                (context.player() as? ServerPlayer)?.let { HotspotUse.use(it, payload.slot) }
            }
        }
        registrar.playToServer(ExitPayload.TYPE, ExitPayload.CODEC) { _, context ->
            context.enqueueWork {
                val player = context.player() as? ServerPlayer ?: return@enqueueWork
                if (player.level().dimension() == PlotManager.DIMENSION) PlotTravel.exit(player)
            }
        }
        registrar.playToServer(HotspotMenuRequestPayload.TYPE, HotspotMenuRequestPayload.CODEC) { _, context ->
            context.enqueueWork {
                (context.player() as? ServerPlayer)?.let { HotspotUse.sendMenu(it) }
            }
        }
        registrar.playToServer(HotspotSetPrimaryPayload.TYPE, HotspotSetPrimaryPayload.CODEC) { payload, context ->
            context.enqueueWork {
                (context.player() as? ServerPlayer)?.let { HotspotUse.setPrimary(it, payload.slot) }
            }
        }
        // Only invoked on the client, so the client-only class is never loaded on a dedicated server.
        registrar.playToClient(HotspotMenuPayload.TYPE, HotspotMenuPayload.CODEC) { payload, context ->
            context.enqueueWork { ClientInit.openHotspotMenu(payload.entries) }
        }
    }
}
