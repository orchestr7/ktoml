package com.akuleshov7.ktoml

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TomlInputConfigTest {
    @Test
    fun currentConstructorKeepsTableRedefinitionOption() {
        assertTrue(TomlInputConfig().allowTableRedefinition)
        assertTrue(TomlInputConfig(ignoreUnknownNames = true).allowTableRedefinition)
        assertFalse(TomlInputConfig(allowTableRedefinition = false).allowTableRedefinition)
    }

    @Test
    fun currentCopyPreservesTableRedefinitionOption() {
        val config = TomlInputConfig.compliant()

        assertEquals(config, config.copy())
        assertFalse(config.copy(ignoreUnknownNames = true).allowTableRedefinition)
        assertFalse(config.copy(false, false, false, true, false, false).allowTableRedefinition)
        assertTrue(config.copy(allowTableRedefinition = true).allowTableRedefinition)
    }
}
