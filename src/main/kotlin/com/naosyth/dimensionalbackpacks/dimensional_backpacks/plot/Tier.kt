package com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot

enum class Tier(val interiorSize: Int) {
    TIER_1(5),
    TIER_2(7),
    TIER_3(9);

    val maxHotspots: Int get() = ordinal + 1

    companion object {
        fun fromOrdinal(ordinal: Int): Tier = entries.getOrElse(ordinal) { TIER_1 }
    }
}
