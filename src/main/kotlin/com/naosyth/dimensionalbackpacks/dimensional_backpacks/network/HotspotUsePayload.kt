package com.naosyth.dimensionalbackpacks.dimensional_backpacks.network

import com.naosyth.dimensionalbackpacks.dimensional_backpacks.DimensionalBackpacks
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

/** Sent by the client when a hotspot keybind is pressed. [slot] is 0-based. */
data class HotspotUsePayload(val slot: Int) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<HotspotUsePayload> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<HotspotUsePayload>(
            ResourceLocation.fromNamespaceAndPath(DimensionalBackpacks.ID, "hotspot_use")
        )
        val CODEC: StreamCodec<ByteBuf, HotspotUsePayload> =
            ByteBufCodecs.VAR_INT.map(::HotspotUsePayload, HotspotUsePayload::slot)
    }
}
