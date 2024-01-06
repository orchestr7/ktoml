package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.Toml
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals

class NestedMapTest {
    @Serializable
    data class NestedTable(
        val outer: Map<String, Map<String, Long>>
    )

    @ExperimentalSerializationApi
    @Test
    fun testDottedKeys() {
        val data = """
            [outer]
                [outer.inner]
                    a = 5
                    b = 5
        """.trimIndent()

        val result = Toml.decodeFromString<NestedTable>(data)
        assertEquals(NestedTable(outer = mapOf("inner" to mapOf("a" to 5, "b" to 5))), result)
    }
}