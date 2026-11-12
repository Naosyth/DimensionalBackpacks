package com.naosyth.dimensionalbackpacks.dimensional_backpacks.storage

internal class CraftOutputLimit(maxStackSize: Int) {
    private var remaining = maxStackSize

    fun allows(count: Int) = count > 0 && count <= remaining

    fun record(count: Int) {
        check(allows(count))
        remaining -= count
    }
}
