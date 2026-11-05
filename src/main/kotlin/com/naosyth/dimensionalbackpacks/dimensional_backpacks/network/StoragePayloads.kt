package com.naosyth.dimensionalbackpacks.dimensional_backpacks.network

import com.naosyth.dimensionalbackpacks.dimensional_backpacks.DimensionalBackpacks
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.StorageGroup
import net.minecraft.core.BlockPos
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack

private fun id(path: String) = ResourceLocation.fromNamespaceAndPath(DimensionalBackpacks.ID, path)

/** Client to server: open the storage screen of the worn backpack. */
object StorageOpenPayload : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<StorageOpenPayload> = TYPE
    val TYPE = CustomPacketPayload.Type<StorageOpenPayload>(id("storage_open"))
    val CODEC: StreamCodec<RegistryFriendlyByteBuf, StorageOpenPayload> = StreamCodec.unit(StorageOpenPayload)
}

/** Client to server: a click in the aggregated item grid. [template] is empty when the click hit no item. */
data class StorageClickPayload(val containerId: Int, val template: ItemStack, val button: Int, val shift: Boolean) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<StorageClickPayload> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<StorageClickPayload>(id("storage_click"))
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, StorageClickPayload> = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StorageClickPayload::containerId,
            ItemStack.OPTIONAL_STREAM_CODEC, StorageClickPayload::template,
            ByteBufCodecs.VAR_INT, StorageClickPayload::button,
            ByteBufCodecs.BOOL, StorageClickPayload::shift,
            ::StorageClickPayload
        )
    }
}

/**
 * Client to server: select, add, remove or rename a storage group, or toggle whether partial stacks are
 * filled before priority insertion. [index] is the group to select for [SELECT]; [name] is the new name
 * of the selected group for [RENAME].
 */
data class StorageGroupPayload(val containerId: Int, val action: Int, val index: Int = 0, val name: String = "") : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<StorageGroupPayload> = TYPE

    companion object {
        const val SELECT = 0
        const val ADD = 1
        const val REMOVE = 2
        const val RENAME = 3
        const val TOGGLE_FILL_PARTIAL_STACKS_FIRST = 4

        val TYPE = CustomPacketPayload.Type<StorageGroupPayload>(id("storage_group"))
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, StorageGroupPayload> = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StorageGroupPayload::containerId,
            ByteBufCodecs.VAR_INT, StorageGroupPayload::action,
            ByteBufCodecs.VAR_INT, StorageGroupPayload::index,
            ByteBufCodecs.stringUtf8(StorageGroup.MAX_NAME), StorageGroupPayload::name,
            ::StorageGroupPayload
        )
    }
}

/** Client to server: the enter button was pressed or released. The server decides when the hold is long enough. */
data class StorageEnterPayload(val containerId: Int, val held: Boolean) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<StorageEnterPayload> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<StorageEnterPayload>(id("storage_enter"))
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, StorageEnterPayload> = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StorageEnterPayload::containerId,
            ByteBufCodecs.BOOL, StorageEnterPayload::held,
            ::StorageEnterPayload
        )
    }
}

data class StorageDepositPayload(val containerId: Int, val matchingOnly: Boolean) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<StorageDepositPayload> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<StorageDepositPayload>(id("storage_deposit"))
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, StorageDepositPayload> = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StorageDepositPayload::containerId,
            ByteBufCodecs.BOOL, StorageDepositPayload::matchingOnly,
            ::StorageDepositPayload
        )
    }
}

/** Client to server: change the storage at [index] in the selected group's priority list. [target] is the new index for [MOVE]. */
data class StorageConfigPayload(val containerId: Int, val index: Int, val action: Int, val target: Int = 0) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<StorageConfigPayload> = TYPE

    companion object {
        const val TOGGLE = 0
        const val UP = 1
        const val DOWN = 2
        const val MOVE = 3

        val TYPE = CustomPacketPayload.Type<StorageConfigPayload>(id("storage_config"))
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, StorageConfigPayload> = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StorageConfigPayload::containerId,
            ByteBufCodecs.VAR_INT, StorageConfigPayload::index,
            ByteBufCodecs.VAR_INT, StorageConfigPayload::action,
            ByteBufCodecs.VAR_INT, StorageConfigPayload::target,
            ::StorageConfigPayload
        )
    }
}

/**
 * Server to client: the storage screen's data. [groups] are all of the plot's groups and [selected] the open
 * one; [items] are the aggregated items of the selected group and [storages] every storage in that group's
 * priority order. [enterHoldTicks] is how long the enter button must be held (0 for no wait).
 */
data class StorageSyncPayload(
    val groups: List<Group>,
    val selected: Int,
    val items: List<Item>,
    val storages: List<Info>,
    val enterHoldTicks: Int
) : CustomPacketPayload {
    /**
     * A group, shown by the block of its top storage ([icon] is empty for a group with none). It has [size]
     * storages with [usedSlots] of [totalSlots] slots occupied.
     */
    data class Group(
        val icon: ItemStack,
        val size: Int,
        val name: String,
        val usedSlots: Int,
        val totalSlots: Int,
        val fillPartialStacksFirst: Boolean
    ) {
        companion object {
            val CODEC: StreamCodec<RegistryFriendlyByteBuf, Group> = StreamCodec.composite(
                ItemStack.OPTIONAL_STREAM_CODEC, Group::icon,
                ByteBufCodecs.VAR_INT, Group::size,
                ByteBufCodecs.stringUtf8(StorageGroup.MAX_NAME), Group::name,
                ByteBufCodecs.VAR_INT, Group::usedSlots,
                ByteBufCodecs.VAR_INT, Group::totalSlots,
                ByteBufCodecs.BOOL, Group::fillPartialStacksFirst,
                ::Group
            )
        }
    }

    data class Item(val stack: ItemStack, val count: Long) {
        companion object {
            val CODEC: StreamCodec<RegistryFriendlyByteBuf, Item> = StreamCodec.composite(
                ItemStack.STREAM_CODEC, Item::stack,
                ByteBufCodecs.VAR_LONG, Item::count,
                ::Item
            )
        }
    }

    /** A storage block. [pos] is relative to the backpack's interior origin; [used] of [size] slots hold items. */
    data class Info(val pos: BlockPos, val icon: ItemStack, val enabled: Boolean, val used: Int, val size: Int) {
        companion object {
            val CODEC: StreamCodec<RegistryFriendlyByteBuf, Info> = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Info::pos,
                ItemStack.OPTIONAL_STREAM_CODEC, Info::icon,
                ByteBufCodecs.BOOL, Info::enabled,
                ByteBufCodecs.VAR_INT, Info::used,
                ByteBufCodecs.VAR_INT, Info::size,
                ::Info
            )
        }
    }

    override fun type(): CustomPacketPayload.Type<StorageSyncPayload> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<StorageSyncPayload>(id("storage_sync"))
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, StorageSyncPayload> = StreamCodec.composite(
            Group.CODEC.apply(ByteBufCodecs.list()), StorageSyncPayload::groups,
            ByteBufCodecs.VAR_INT, StorageSyncPayload::selected,
            Item.CODEC.apply(ByteBufCodecs.list()), StorageSyncPayload::items,
            Info.CODEC.apply(ByteBufCodecs.list()), StorageSyncPayload::storages,
            ByteBufCodecs.VAR_INT, StorageSyncPayload::enterHoldTicks,
            ::StorageSyncPayload
        )
    }
}
