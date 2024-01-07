package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.exceptions.IllegalTypeException
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NestedMapTest {
    @Serializable
    data class NestedTable(
        val outer: Map<String, Map<String, Long>>
    )

    @Serializable
    data class SimpleNestedTable(
        val map: Map<String, Long>,
    )

    @ExperimentalSerializationApi
    @Test
    fun nestedInvalidMapping() {
        val data = """
            [map]
                [map.a]
                     b = 1
               
        """.trimIndent()

        assertFailsWith<IllegalTypeException> {
            Toml.decodeFromString<SimpleNestedTable>(data)
        }
    }

    @ExperimentalSerializationApi
    @Test
    fun testSimpleNestedMaps() {
        val data = """
            [outer]
                [outer.inner1]
                    a = 5
                    b = 5
                    
        """.trimIndent()

        val result = Toml.decodeFromString<NestedTable>(data)
        assertEquals(NestedTable(outer = mapOf("inner1" to mapOf("a" to 5, "b" to 5))), result)
    }

    @ExperimentalSerializationApi
    @Test
    fun testNestedMaps() {
        val data = """
            [outer]
                [outer.inner1]
                    a = 5
                    b = 5
                [outer.inner2]
                    c = 7
                    d = 12
                    
        """.trimIndent()

        val result = Toml.decodeFromString<NestedTable>(data)
        assertEquals(NestedTable(outer = mapOf("inner1" to mapOf("a" to 5, "b" to 5), "inner2" to mapOf("c" to 7, "d" to 12))), result)
    }

    @ExperimentalSerializationApi
    @Test
    fun nestedMapFromReadme() {
        @Serializable
        data class MyClass(
            val a: Map<String, Map<String, String>>
        )

        val data = """
            [a]
                b = 42
                c = "String"
                [a.innerTable]
                    d = 5
                [a.otherInnerTable]
                    d = "String"
        """.trimIndent()

        println(Toml.decodeFromString<MyClass>(data))
    }
}