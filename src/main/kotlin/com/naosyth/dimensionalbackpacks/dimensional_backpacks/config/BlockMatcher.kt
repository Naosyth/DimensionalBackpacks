package com.naosyth.dimensionalbackpacks.dimensional_backpacks.config

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState

/**
 * A list of blocks parsed from config strings. Accepted entries:
 * - `modid:block` for one block
 * - `modid:*` (or just `modid`) for every block of a mod
 * - `#modid:tag` for a block tag
 */
class BlockMatcher(entries: List<String>) {
    private val blocks = HashSet<ResourceLocation>()
    private val namespaces = HashSet<String>()
    private val tags = ArrayList<TagKey<Block>>()

    init {
        for (raw in entries) {
            val entry = raw.trim()
            when {
                entry.isEmpty() -> {}
                entry.startsWith("#") -> ResourceLocation.tryParse(entry.substring(1))?.let { tags += TagKey.create(Registries.BLOCK, it) }
                !entry.contains(':') -> namespaces += entry
                entry.endsWith(":*") -> namespaces += entry.removeSuffix(":*")
                else -> ResourceLocation.tryParse(entry)?.let { blocks += it }
            }
        }
    }

    fun matches(state: BlockState): Boolean {
        val id = BuiltInRegistries.BLOCK.getKey(state.block)
        return id in blocks || id.namespace in namespaces || tags.any { state.`is`(it) }
    }
}
