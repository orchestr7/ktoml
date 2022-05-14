package com.akuleshov7.ktoml.writers

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.TomlOutputConfig
import kotlin.test.Test
import kotlin.test.assertEquals

class ArrayOfTablesWriteTest {
    @Test
    fun flatTableArrayWriteTest() {
        val toml = """
            [[fruits]]
                a = "apple"
                b = "qwerty"
            
            [[fruits]]
                a = "banana"
                b = "qwerty"
            
            [[fruits]]
                a = "plantain"
                b = "qwerty"
        """.trimIndent()

        testTableArray(toml)
    }

    @Test
    fun flatEmptyTableArrayWriteTest() {
        val toml = """
            [[products]]
                name = "Hammer"
                sku = 738594937
            
            [[products]]
            
            [[products]]
                name = "Nail"
                sku = 284758393
                color = "gray"
        """.trimIndent()

        testTableArray(toml)
    }

    @Test
    fun nestedTableArrayWriteTest() {
        val toml = """
            [[fruits.varieties]]
                name = "red delicious"
            
                [[fruits.varieties.inside]]
                    name = "granny smith"
        """.trimIndent()

        testTableArray(toml)
    }

    @Test
    fun tableArrayWithNestedTableWriteTest1() {
        val toml = """
            [[fruits.varieties]]
                name = "red delicious"
            
                [fruits.varieties.inside]
                    name = "granny smith"
        """.trimIndent()

        testTableArray(toml)
    }

    @Test
    fun tableArrayWithNestedTableWriteTest2() {
        val toml = """
            [[fruit]]
                [fruit.physical]
                    color = "red"
                    shape = "round"
            
            [[fruit]]
                [fruit.physical]
                    color = "red"
                    shape = "round"
        """.trimIndent()

        testTableArray(toml)
    }

    @Test
    fun dottedFlatTableArrayWriteTest() {
        val toml = """
            [[fruits.varieties]]
                name = "red delicious"
            
            [[fruits.varieties]]
                name = "granny smith"
            
            [[fruits.varieties]]
                name = "granny smith"
        """.trimIndent()

        testTableArray(toml)
    }

    @Test
    fun complexTableArrayWriteTest1() {
        val toml = """
            [[a.b]]
                name = 1
            
                [a.b.c]
                    name = 2
            
            [[a.b]]
                name = 3
            
                [a.b.c]
                    name = 4
            
            [[c]]
                name = 5
        """.trimIndent()

        testTableArray(toml)
    }

    @Test
    fun complexTableArrayWriteTest2() {
        val toml = """
            [a]
                name = 1
            
                [[a.b.c]]
                    name = 2
            
                [[a.b.c]]
                    name = 4
        """.trimIndent()

        testTableArray(toml)
    }
}

fun testTableArray(
    expected: String,
    inputConfig: TomlInputConfig = TomlInputConfig(),
    outputConfig: TomlOutputConfig = TomlOutputConfig()
) {
    val file = Toml(inputConfig).tomlParser.parseString(expected)

    file.prettyPrint()

    assertEquals(
        expected,
        buildString(expected.length) {
            val emitter = TomlStringEmitter(this, outputConfig)

            file.write(emitter, outputConfig)
        }
    )
}