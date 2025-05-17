package com.akuleshov7.ktoml.decoders.tables

import com.akuleshov7.ktoml.Toml
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals

class NestedTableTest {
    @Serializable
    data class NestedTable(
        val outer: Outer
    )

    @Serializable
    data class Outer(
        val inner: Inner
    )

    @Serializable
    data class Inner(
        val a: Int
    )

    @ExperimentalSerializationApi
    @Test
    fun testDottedKeys() {
        val data = """
            [outer]
                [outer.inner]
                    a = 5
        """.trimIndent()

        val result = Toml.decodeFromString<NestedTable>(data)
        assertEquals(NestedTable(Outer(Inner(a = 5))), result)
    }
}