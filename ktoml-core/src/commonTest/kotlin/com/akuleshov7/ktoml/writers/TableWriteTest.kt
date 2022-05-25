package com.akuleshov7.ktoml.writers

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.TomlOutputConfig
import kotlin.test.Test
import kotlin.test.assertEquals

class TableWriteTest {
    @Test
    fun emptyNestedTableWriteTest() {
        val toml = """
            [a.b]
            
            [a]
                c = 3
        """.trimIndent()

        testTable(toml)
    }

    @Test
    fun nestedTableWriteTest() {
        val toml = """
            [a]
                b = 3
            
                [a.c]
                    d = 5
        """.trimIndent()

        testTable(toml)
    }

    @Test
    fun emptyTableWriteTest() {
        val toml = """
            [test]
        """.trimIndent()

        testTable(toml)
    }

    @Test
    fun flatTableWriteTest() {
        val toml = """
            [a]
            
            [b]
                c = 4
            
            [c]
                d = 7
        """.trimIndent()

        testTable(toml)
    }

    @Test
    fun complexTableWriteTest() {
        val toml = """
            [a]
                name = 1
            
                [a.b]
                    name = 2
            
            [c.a.b]
                name = 3
            
                [c.a.b.a.b.c]
                    test = 3
            
            [c]
                name = 5
        """.trimIndent()

        testTable(toml)
    }
}

fun testTable(
    expected: String,
    inputConfig: TomlInputConfig = TomlInputConfig(),
    outputConfig: TomlOutputConfig = TomlOutputConfig()
) {
    val file = Toml(inputConfig, outputConfig).tomlParser.parseString(expected)

    file.prettyPrint()

    assertEquals(
        expected,
        buildString(expected.length) {
            val emitter = TomlStringEmitter(this, outputConfig)

            file.write(emitter, outputConfig)
        }
    )
}