package com.naosyth.dimensionalbackpacks.dimensional_backpacks.config

import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.Tier
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.common.ModConfigSpec

/** Server-side settings, in `config/dimensional_backpacks-common.toml`. */
object ModConfig {
    /** Highest hotspot count a tier may be configured with. */
    const val MAX_HOTSPOTS = 8

    private val builder = ModConfigSpec.Builder()

    private val hotspotCounts: Map<Tier, ModConfigSpec.IntValue>

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
    private val hotspotRule: BlockRule

    init {
        builder.comment("How many hotspots each backpack tier supports. 0 disables hotspots for that tier.")
            .push("hotspots")
        hotspotCounts = Tier.entries.associateWith { tier ->
            builder.defineInRange("tier${tier.ordinal + 1}", tier.ordinal + 1, 0, MAX_HOTSPOTS)
        }
        builder.pop()

        val formats = "Entries: \"modid:block\" for one block, \"modid:*\" for a whole mod, \"#modid:tag\" for a block tag."
        builder.comment("Which blocks may be placed in a backpack, and which can be used through hotspots.")
            .push("allowed_blocks")
        placementRule = defineRule(
            "placement", "Blocks that can be placed inside a backpack.",
            ListMode.BLACKLIST, emptyList(), formats
        )
        hotspotRule = defineRule(
            "hotspot",
            "Blocks that can be used through a hotspot. Many mods' menus need the block nearby or loaded on the client, " +
                "and fail or crash from a hotspot, so only whitelist mods you've tested.",
            ListMode.WHITELIST, listOf("minecraft", "refinedstorage"), formats
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

    val SPEC: ModConfigSpec = builder.build()

    fun maxHotspots(tier: Tier): Int = hotspotCounts.getValue(tier).get()

    fun isPlacementBlocked(state: BlockState): Boolean = !placementRule.allows(state)

    fun isHotspotBlocked(state: BlockState): Boolean = !hotspotRule.allows(state)
}
