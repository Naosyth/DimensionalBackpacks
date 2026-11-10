package com.naosyth.dimensionalbackpacks.dimensional_backpacks.storage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RecipeTransferMathTest {
    @Test
    fun `nonstackable ingredient limits all slots to one complete set`() {
        assertEquals(1, maximumRecipeSets(listOf(RecipeSupply(1, 1000, 1), RecipeSupply(3, 1000, 64))))
    }

    @Test
    fun `duplicate ingredient counts are shared across its slots`() {
        assertEquals(21, maximumRecipeSets(listOf(RecipeSupply(3, 64, 64), RecipeSupply(1, 100, 64))))
    }

    @Test
    fun `very large supplies never exceed grid slot limits`() {
        assertEquals(16, maximumRecipeSets(listOf(RecipeSupply(1, Long.MAX_VALUE, 16), RecipeSupply(1, Long.MAX_VALUE, 64))))
    }

    @Test
    fun `missing ingredient prevents a complete set`() {
        assertEquals(0, maximumRecipeSets(listOf(RecipeSupply(2, 1, 64), RecipeSupply(1, 10, 64))))
    }
}
