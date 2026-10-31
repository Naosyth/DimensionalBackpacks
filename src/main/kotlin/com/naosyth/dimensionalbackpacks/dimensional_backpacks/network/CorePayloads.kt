package com.naosyth.dimensionalbackpacks.dimensional_backpacks.network

import com.naosyth.dimensionalbackpacks.dimensional_backpacks.DimensionalBackpacks
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

/** Client to server: leave the backpack the player is currently inside. */
object ExitPayload : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<ExitPayload> = TYPE

    val TYPE = CustomPacketPayload.Type<ExitPayload>(
        ResourceLocation.fromNamespaceAndPath(DimensionalBackpacks.ID, "exit")
    )

    // unit() only accepts this exact instance, so the payload is a singleton.
    val CODEC: StreamCodec<RegistryFriendlyByteBuf, ExitPayload> = StreamCodec.unit(ExitPayload)
}

/** Server to client: the tint of a plot's interior walls, or [NONE] for undyed. */
data class PlotColorPayload(val plotId: Int, val color: Int) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<PlotColorPayload> = TYPE

    companion object {
        const val NONE = -1
        val TYPE = CustomPacketPayload.Type<PlotColorPayload>(
            ResourceLocation.fromNamespaceAndPath(DimensionalBackpacks.ID, "plot_color")
        )
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, PlotColorPayload> = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PlotColorPayload::plotId,
            ByteBufCodecs.INT, PlotColorPayload::color,
            ::PlotColorPayload
        )
    }
}
