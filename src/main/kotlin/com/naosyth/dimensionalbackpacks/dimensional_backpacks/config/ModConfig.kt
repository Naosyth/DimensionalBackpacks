package com.naosyth.dimensionalbackpacks.dimensional_backpacks.config

import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.StorageGroup
import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.Tier
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.common.ModConfigSpec

/** Server-side settings, in `config/dimensional_backpacks-common.toml`. */
object ModConfig {
    private val builder = ModConfigSpec.Builder()

    enum class ListMode { WHITELIST, BLACKLIST }

    /** A mode plus a block list; matchers are rebuilt only when the config's contents change. */
    private class BlockRule(
        private val mode: ModConfigSpec.EnumValue<ListMode>,
        private val entries: ModConfigSpec.ConfigValue<List<String>>,
    ) {
        private var source: List<String>? = null
        private var matcher = BlockMatcher(emptyList())

        fun allows(state: BlockState): Boolean {
            val list = entries.get()
            if (list != source) {
                source = list
                matcher = BlockMatcher(list)
            }
            val listed = matcher.matches(state)
            return if (mode.get() == ListMode.WHITELIST) listed else !listed
        }
    }

    private val placementRule: BlockRule

    init {
        val formats = "Entries: \"modid:block\" for one block, \"modid:*\" for a whole mod, \"#modid:tag\" for a block tag."
        builder.comment("Which blocks may be placed in a backpack.")
            .push("allowed_blocks")
        placementRule = defineRule(
            "placement", "Blocks that can be placed inside a backpack.",
            ListMode.BLACKLIST, emptyList(), formats
        )
        builder.pop()
    }

    private fun defineRule(name: String, description: String, mode: ListMode, entries: List<String>, formats: String): BlockRule {
        builder.comment(description).push(name)
        val modeValue = builder
            .comment("WHITELIST: only listed blocks are allowed. BLACKLIST: listed blocks are not allowed.")
            .defineEnum("mode", mode)
        val list = builder
            .comment("The blocks the mode applies to.", formats)
            .defineListAllowEmpty("blocks", entries) { it is String }
        builder.pop()
        return BlockRule(modeValue, list)
    }

    private val copyingEnabled = builder
        .comment("Whether crafting a linked backpack with a blank one of the same tier makes another backpack linked to the same interior.")
        .define("allow_backpack_copying", true)

    private val containerLimitsEnabled: ModConfigSpec.BooleanValue
    private val containerLimits: List<ModConfigSpec.IntValue>
    private val groupLimitsEnabled: ModConfigSpec.BooleanValue
    private val groupLimits: List<ModConfigSpec.IntValue>

    init {
        val containerDefaults = listOf(3, 6, 10)
        val groupDefaults = listOf(3, 6, 9)

        builder.comment("Limits on how many storages can be enabled in one storage group.").push("storages_per_group")
        containerLimitsEnabled = builder
            .comment("Whether the per-tier limits below are enforced. When false, groups can hold any number of storages.")
            .define("enabled", false)
        containerLimits = Tier.entries.map {
            builder.comment("Max storages in one group for tier ${it.ordinal + 1} backpacks.")
                .defineInRange("tier_${it.ordinal + 1}", containerDefaults[it.ordinal], 1, 1000)
        }
        builder.pop()

        builder.comment("Limits on how many storage groups a backpack can have.").push("groups_per_backpack")
        groupLimitsEnabled = builder
            .comment("Whether the per-tier limits below are enforced. When false, backpacks can have up to ${StorageGroup.MAX} groups.")
            .define("enabled", false)
        groupLimits = Tier.entries.map {
            builder.comment("Max groups for tier ${it.ordinal + 1} backpacks (at most ${StorageGroup.MAX}).")
                .defineInRange("tier_${it.ordinal + 1}", groupDefaults[it.ordinal], 1, StorageGroup.MAX)
        }
        builder.pop()
    }

    val SPEC: ModConfigSpec = builder.build()

    fun isCopyingEnabled(): Boolean = !SPEC.isLoaded || copyingEnabled.get()

    /** Max enabled storages in one group of a [tier] backpack. */
    fun maxStoragesPerGroup(tier: Tier): Int =
        if (!SPEC.isLoaded || !containerLimitsEnabled.get()) Int.MAX_VALUE else containerLimits[tier.ordinal].get()

    /** Max groups in a [tier] backpack, never above [StorageGroup.MAX]. */
    fun maxGroups(tier: Tier): Int =
        if (!SPEC.isLoaded || !groupLimitsEnabled.get()) StorageGroup.MAX else groupLimits[tier.ordinal].get()

    fun isPlacementBlocked(state: BlockState): Boolean = !placementRule.allows(state)
}
