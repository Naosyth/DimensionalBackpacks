package com.naosyth.dimensionalbackpacks.dimensional_backpacks.client.storage

import com.naosyth.dimensionalbackpacks.dimensional_backpacks.network.StorageConfigPayload
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.network.StorageDepositPayload
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.network.StorageEnterPayload
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.network.StorageGroupPayload
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.network.StorageClickPayload
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.network.StorageSyncPayload
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.storage.StorageMenu
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.common.ModConfigSpec
import net.neoforged.neoforge.network.PacketDistributor
import java.util.Collections

/** What the server last told the screen: the plot's groups and the selected group's items and storages. */
internal class StorageModel {
    var received = false
        private set
    var groups: List<StorageSyncPayload.Group> = emptyList()
        private set
    var selected = 0
        private set
    var items: List<StorageSyncPayload.Item> = emptyList()
        private set
    var storages: List<StorageSyncPayload.Info> = emptyList()
        private set
    var enterHoldTicks = 0
        private set
    var totalSlots: Int = 0
        get() = storages.filter { it.enabled }.sumOf { it.size }
        private set

    var usedSlots: Int = 0
        get() = storages.filter { it.enabled }.sumOf { it.used }
        private set

    /** The group's name, or "Group N" while it has none. */
    fun groupName(index: Int) = groups.getOrNull(index)?.name?.takeIf { it.isNotBlank() } ?: "Group ${index + 1}"

    fun update(payload: StorageSyncPayload) {
        received = true
        groups = payload.groups
        selected = payload.selected
        items = payload.items
        storages = payload.storages
        enterHoldTicks = payload.enterHoldTicks
    }

    /** Mirrors a [StorageConfigPayload] right away so the list doesn't flicker while waiting for the sync. */
    fun applyConfig(index: Int, action: Int, target: Int) {
        val list = storages.toMutableList()
        when (action) {
            StorageConfigPayload.TOGGLE -> list[index] = list[index].copy(enabled = !list[index].enabled)
            StorageConfigPayload.UP -> if (index > 0) Collections.swap(list, index, index - 1)
            StorageConfigPayload.DOWN -> if (index < list.size - 1) Collections.swap(list, index, index + 1)
            StorageConfigPayload.MOVE -> list.add(target, list.removeAt(index))
        }
        storages = list
    }
}

/** What the screen can ask of the server. */
internal class StorageActions(private val menu: StorageMenu) {
    fun click(template: ItemStack, button: Int, shift: Boolean) =
        PacketDistributor.sendToServer(StorageClickPayload(menu.containerId, template, button, shift))

    fun config(index: Int, action: Int, target: Int = 0) =
        PacketDistributor.sendToServer(StorageConfigPayload(menu.containerId, index, action, target))

    fun selectGroup(index: Int) = group(StorageGroupPayload.SELECT, index)
    fun addGroup() = group(StorageGroupPayload.ADD)
    fun removeGroup() = group(StorageGroupPayload.REMOVE)
    fun renameGroup(name: String) = group(StorageGroupPayload.RENAME, name = name)
    fun toggleFillPartialStacksFirst() = group(StorageGroupPayload.TOGGLE_FILL_PARTIAL_STACKS_FIRST)
    fun setEnterHeld(held: Boolean) = PacketDistributor.sendToServer(StorageEnterPayload(menu.containerId, held))
    fun deposit(matchingOnly: Boolean) =
        PacketDistributor.sendToServer(StorageDepositPayload(menu.containerId, matchingOnly))

    private fun group(action: Int, index: Int = 0, name: String = "") =
        PacketDistributor.sendToServer(StorageGroupPayload(menu.containerId, action, index, name))
}

enum class SortBy { NAME, COUNT }

/** Sort choice, saved in the client config so it survives restarts. */
internal object StoragePrefs {
    private val builder = ModConfigSpec.Builder()
    private val sortByValue = builder.comment("Storage screen sort field").defineEnum("sortBy", SortBy.NAME)
    private val ascendingValue = builder.comment("Storage screen sort direction").define("ascending", true)
    val SPEC: ModConfigSpec = builder.build()

    var sortBy: SortBy
        get() = if (SPEC.isLoaded) sortByValue.get() else SortBy.NAME
        set(value) {
            sortByValue.set(value)
            SPEC.save()
        }

    var ascending: Boolean
        get() = if (SPEC.isLoaded) ascendingValue.get() else true
        set(value) {
            ascendingValue.set(value)
            SPEC.save()
        }
}
