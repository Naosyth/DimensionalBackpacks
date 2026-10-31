package com.naosyth.dimensionalbackpacks.dimensional_backpacks.config

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

    val SPEC: ModConfigSpec = builder.build()

    fun isCopyingEnabled(): Boolean = !SPEC.isLoaded || copyingEnabled.get()

    fun isPlacementBlocked(state: BlockState): Boolean = !placementRule.allows(state)
}
