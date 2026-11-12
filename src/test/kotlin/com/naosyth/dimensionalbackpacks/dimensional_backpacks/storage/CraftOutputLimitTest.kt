package com.naosyth.dimensionalbackpacks.dimensional_backpacks.storage

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CraftOutputLimitTest {
    @Test
    fun `limits repeated single output crafts to one stack`() {
        val limit = CraftOutputLimit(64)
        repeat(64) {
            assertTrue(limit.allows(1))
            limit.record(1)
        }
        assertFalse(limit.allows(1))
    }

    @Test
    fun `never splits a craft that would exceed the limit`() {
        val limit = CraftOutputLimit(64)
        repeat(21) { limit.record(3) }
        assertFalse(limit.allows(3))
        assertTrue(limit.allows(1))
    }

    @Test
    fun `respects smaller stack limits and a fresh click resets the limit`() {
        val limit = CraftOutputLimit(16)
        repeat(4) { limit.record(4) }
        assertFalse(limit.allows(4))
        assertFalse(limit.allows(0))
        assertTrue(CraftOutputLimit(16).allows(4))
    }
}
