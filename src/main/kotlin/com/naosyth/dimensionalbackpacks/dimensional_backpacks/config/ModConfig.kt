package com.naosyth.dimensionalbackpacks.dimensional_backpacks.config

import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.Tier
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.common.ModConfigSpec

/** Server-side settings, in `config/dimensional_backpacks-common.toml`. */
object ModConfig {
    /** Highest hotspot count a tier may be configured with. */
    const val MAX_HOTSPOTS = 3

    private val builder = ModConfigSpec.Builder()

    private val hotspotCounts: Map<Tier, ModConfigSpec.IntValue>

    private val placementBlacklist: ModConfigSpec.ConfigValue<List<String>>
    private val hotspotBlacklist: ModConfigSpec.ConfigValue<List<String>>
    private val useWhitelist: ModConfigSpec.BooleanValue
    private val hotspotWhitelist: ModConfigSpec.ConfigValue<List<String>>

    init {
        builder.comment("How many hotspots each backpack tier supports. 0 disables hotspots for that tier.")
            .push("hotspots")
        hotspotCounts = Tier.entries.associateWith { tier ->
            builder.defineInRange("tier${tier.ordinal + 1}", tier.ordinal + 1, 0, MAX_HOTSPOTS)
        }
        builder.pop()

        val formats = "Entries: \"modid:block\" for one block, \"modid:*\" for a whole mod, \"#modid:tag\" for a block tag."
        builder.push("blacklists")
        placementBlacklist = builder
            .comment("Blocks that can't be placed inside a backpack.", formats)
            .defineListAllowEmpty("placement", emptyList<String>()) { it is String }
        hotspotBlacklist = builder
            .comment("Blocks that can be placed inside a backpack but can't be used through a hotspot.", formats)
            .defineListAllowEmpty("hotspot", listOf("ae2", "toms_storage", "mekanism", "sophisticatedstorage")) { it is String }
        builder.pop()

        builder.push("hotspot_access")
        useWhitelist = builder
            .comment(
                "If true, hotspots only work on blocks listed in 'whitelist' (the blacklist is still applied after).",
                "Many mods open menus that need the block to be near the player or loaded on the client, and fail or crash from a hotspot.",
                "Only add mods you've tested."
            )
            .define("use_whitelist", true)
        hotspotWhitelist = builder
            .comment("Blocks usable through hotspots when use_whitelist is true.", formats)
            .defineListAllowEmpty("whitelist", listOf("minecraft", "refinedstorage")) { it is String }
        builder.pop()
    }

    val SPEC: ModConfigSpec = builder.build()

    // Matchers are rebuilt only when the config's contents change.
    private var placementSource: List<String>? = null
    private var placementMatcher = BlockMatcher(emptyList())
    private var hotspotSource: List<String>? = null
    private var hotspotMatcher = BlockMatcher(emptyList())

    fun maxHotspots(tier: Tier): Int = hotspotCounts.getValue(tier).get()

    fun isPlacementBlocked(state: BlockState): Boolean {
        val list = placementBlacklist.get()
        if (list != placementSource) {
            placementSource = list
            placementMatcher = BlockMatcher(list)
        }
        return placementMatcher.matches(state)
    }

    private var whitelistSource: List<String>? = null
    private var whitelistMatcher = BlockMatcher(emptyList())

    fun isHotspotBlocked(state: BlockState): Boolean {
        if (useWhitelist.get()) {
            val allowed = hotspotWhitelist.get()
            if (allowed != whitelistSource) {
                whitelistSource = allowed
                whitelistMatcher = BlockMatcher(allowed)
            }
            if (!whitelistMatcher.matches(state)) return true
        }
        val list = hotspotBlacklist.get()
        if (list != hotspotSource) {
            hotspotSource = list
            hotspotMatcher = BlockMatcher(list)
        }
        return hotspotMatcher.matches(state)
    }
}
