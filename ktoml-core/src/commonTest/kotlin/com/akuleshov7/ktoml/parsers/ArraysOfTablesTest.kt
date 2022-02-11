package com.akuleshov7.ktoml.parsers

import com.akuleshov7.ktoml.Toml.Default.tomlParser
import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.tree.TableType
import com.akuleshov7.ktoml.tree.TomlFile
import com.akuleshov7.ktoml.tree.TomlKey
import com.akuleshov7.ktoml.tree.TomlKeyValuePrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ArraysOfTablesTest {
    @Test
    fun positiveSimpleParsingTest() {
        val string = """
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

        val parsedToml = tomlParser.parseString(string)
        parsedToml.prettyPrint()
        val array = parsedToml.findTableInAstByName("fruits", 1, TableType.ARRAY)
        assertEquals(3, array?.children?.size)
    }

    @Test
    fun tomlExampleParsingTest() {
        val string = """
            [[fruits]]
            name = "apple"
            
            [fruits.physical]
            color = "red"
            shape = "round"
            
            [[fruits.varieties]] 
            name = "red delicious"
            
            [[fruits.varieties]]
            name = "granny smith"
            
            [[fruits]]
            name = "banana"
            
            [[fruits.varieties]]
            name = "plantain"
        """.trimIndent()

        val parsedToml = tomlParser.parseString(string)
        parsedToml.prettyPrint()
        val array = parsedToml.findTableInAstByName("fruits.varieties", 2, TableType.ARRAY)
        assertEquals(3, array?.children?.size)

    }

    @Test
    fun parsingRegression() {
        val string = """
            [fruits]
            name = "apple"

            [fruits]
            name = "banana"

            [fruits]
            name = "plantain"
        """.trimIndent()

        val parsedToml = tomlParser.parseString(string)
        parsedToml.prettyPrint()
        assertEquals(parsedToml.children.size, 2)
    }
}
