package com.naosyth.dimensionalbackpacks.dimensional_backpacks.storage

internal data class RecipeSupply(val slots: Int, val available: Long, val slotLimit: Int)

internal fun maximumRecipeSets(supplies: List<RecipeSupply>): Int =
    supplies.minOf { minOf(it.slotLimit.toLong(), it.available / it.slots).toInt() }
