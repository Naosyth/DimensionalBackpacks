package com.naosyth.dimensionalbackpacks.dimensional_backpacks.client.compat

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RecipeAssignmentTest {
    @Test
    fun `reassigns flexible choices to leave a scarce item for a restricted slot`() {
        assertArrayEquals(
            intArrayOf(1, 0),
            RecipeAssignment.assign(listOf(1, 1), listOf(listOf(0, 1), listOf(0)))
        )
    }

    @Test
    fun `respects counts and leaves unavailable slots unassigned`() {
        assertArrayEquals(
            intArrayOf(0, 0, -1, -1),
            RecipeAssignment.assign(listOf(2, 0), listOf(listOf(0), listOf(0), listOf(0), listOf(1)))
        )
    }

    @Test
    fun `reassigns through multiple occupied alternatives`() {
        assertArrayEquals(
            intArrayOf(1, 2, 0, -1),
            RecipeAssignment.assign(listOf(1, 1, 1), listOf(listOf(0, 1), listOf(1, 2), listOf(0), emptyList()))
        )
    }

    @Test
    fun `maximizes assigned slots for every small choice graph`() {
        fun best(candidates: List<List<Int>>, capacities: IntArray, slot: Int): Int {
            if (slot == candidates.size) return 0
            var result = best(candidates, capacities, slot + 1)
            for (index in candidates[slot]) {
                if (capacities[index] == 0) continue
                capacities[index]--
                result = maxOf(result, 1 + best(candidates, capacities, slot + 1))
                capacities[index]++
            }
            return result
        }

        for (capacityMask in 0 until 27) {
            val capacities = listOf(capacityMask % 3, capacityMask / 3 % 3, capacityMask / 9)
            for (edges in 0 until (1 shl 9)) {
                val candidates = List(3) { slot -> (0 until 3).filter { edges and (1 shl (slot * 3 + it)) != 0 } }
                val assigned = RecipeAssignment.assign(capacities, candidates)
                assertEquals(best(candidates, capacities.toIntArray(), 0), assigned.count { it >= 0 })
                for (index in capacities.indices) {
                    assertEquals(true, assigned.count { it == index } <= capacities[index])
                }
            }
        }
    }
}
