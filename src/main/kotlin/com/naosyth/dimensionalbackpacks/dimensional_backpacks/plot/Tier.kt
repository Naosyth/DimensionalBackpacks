package com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot

enum class Tier(val interiorSize: Int) {
    TIER_1(5),
    TIER_2(11),
    TIER_3(15);

    val maxHotspots: Int get() = ordinal + 1

    companion object {
        fun fromOrdinal(ordinal: Int): Tier = entries.getOrElse(ordinal) { TIER_1 }
    }
}
