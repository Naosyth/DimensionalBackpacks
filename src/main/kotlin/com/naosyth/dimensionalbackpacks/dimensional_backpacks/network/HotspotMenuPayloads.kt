package com.naosyth.dimensionalbackpacks.dimensional_backpacks.network

import com.naosyth.dimensionalbackpacks.dimensional_backpacks.DimensionalBackpacks
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack

/** Client to server: leave the backpack the player is currently inside. */
object ExitPayload : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<ExitPayload> = TYPE

    val TYPE = CustomPacketPayload.Type<ExitPayload>(
        ResourceLocation.fromNamespaceAndPath(DimensionalBackpacks.ID, "exit")
    )

    val CODEC: StreamCodec<RegistryFriendlyByteBuf, ExitPayload> = StreamCodec.unit(ExitPayload)
}

/** Client to server: the hotkey was held, so send back the hotspots to choose from. */
object HotspotMenuRequestPayload : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<HotspotMenuRequestPayload> = TYPE

    val TYPE = CustomPacketPayload.Type<HotspotMenuRequestPayload>(
        ResourceLocation.fromNamespaceAndPath(DimensionalBackpacks.ID, "hotspot_menu_request")
    )

    // unit() only accepts this exact instance, so the payload is a singleton.
    val CODEC: StreamCodec<RegistryFriendlyByteBuf, HotspotMenuRequestPayload> = StreamCodec.unit(HotspotMenuRequestPayload)
}

/** Client to server: make the hotspot in [slot] the primary one. */
data class HotspotSetPrimaryPayload(val slot: Int) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<HotspotSetPrimaryPayload> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<HotspotSetPrimaryPayload>(
            ResourceLocation.fromNamespaceAndPath(DimensionalBackpacks.ID, "hotspot_set_primary")
        )
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, HotspotSetPrimaryPayload> =
            ByteBufCodecs.VAR_INT.map(::HotspotSetPrimaryPayload, HotspotSetPrimaryPayload::slot).cast()
    }
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

/** Server to client: the worn backpack's hotspots, each shown as the block it touches. */
data class HotspotMenuPayload(val entries: List<Entry>) : CustomPacketPayload {
    data class Entry(val slot: Int, val icon: ItemStack, val primary: Boolean) {
        companion object {
            val CODEC: StreamCodec<RegistryFriendlyByteBuf, Entry> = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Entry::slot,
                ItemStack.OPTIONAL_STREAM_CODEC, Entry::icon,
                ByteBufCodecs.BOOL, Entry::primary,
                ::Entry
            )
        }
    }

    override fun type(): CustomPacketPayload.Type<HotspotMenuPayload> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<HotspotMenuPayload>(
            ResourceLocation.fromNamespaceAndPath(DimensionalBackpacks.ID, "hotspot_menu")
        )
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, HotspotMenuPayload> =
            Entry.CODEC.apply(ByteBufCodecs.list()).map(::HotspotMenuPayload, HotspotMenuPayload::entries)
    }
}
