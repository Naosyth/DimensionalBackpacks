package com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StorageGroupTest {
    @Test
    fun `new groups fill partial stacks first by default`() {
        assertTrue(StorageGroup().fillPartialStacksFirst)
        assertFalse(StorageGroup(fillPartialStacksFirst = false).fillPartialStacksFirst)
    }
}
