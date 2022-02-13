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
        val array = parsedToml.findTableInAstByName("fruits", 1)
        assertEquals(3, array?.children?.size)
    }

    @Test
    fun tomlExampleParsingTest() {
        val string = """
            [[fruits]]
                name = "apple"
            
            # this is processed incorrectly now
            [fruits.physical]
                color = "red"
                shape = "round"
            
            # this is processed incorrectly now
            [fruits.physical.inside]
                color = "red"
                shape = "round"
               
            # this is processed incorrectly now
            [vegetables]
                outSideOfArray = true
            
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
        val array = parsedToml.findTableInAstByName("fruits.varieties", 2)
        assertEquals(3, array?.children?.size)

        assertTrue { false }

        """
        [[fruit]]
        [fruit.physical]  
        color = "red"
        shape = "round"
        
        [[fruit]]
        [fruit.physical]  
        color = "red"
        shape = "round"
        """.trimIndent()

    }


    @Test
    fun parsingNestedArraysOfTablesRegression2() {
        val string = """
            [[products]]
                name = "Hammer"
                sku = 738594937
        
            [[products]]  
        
            [[products]]
                name = "Nail"
                sku = 284758393
            
                color = "gray"
        """.trimIndent()

        val parsedToml = tomlParser.parseString(string)
        parsedToml.prettyPrint()

        assertTrue { false }
    }



    @Test
    fun parsingNestedArraysOfTablesRegression() {
        val string = """
            # bug here
            [[fruits]]
                name = "banana"
            
            [[fruits.varieties]]
                name = "granny smith"
            
            [[fruits]]
                name = "banana"
            
            [[fruits.varieties]]
                name = "plantain"
        """.trimIndent()

        val parsedToml = tomlParser.parseString(string)
        parsedToml.prettyPrint()

        assertTrue { false }
    }

    @Test
    fun parsingNestedArraysOfTablesRegression1() {
        val string = """
            # bug here
            [[fruits.varieties]] 
                name = "red delicious"
            
            [[fruits.varieties]]
                name = "granny smith"
        """.trimIndent()

        val parsedToml = tomlParser.parseString(string)
        parsedToml.prettyPrint()

        assertTrue { false }
    }


    @Test
    fun parsingNestedArraysOfTablesTest() {
        val string = """
            [[fruits.varieties]] 
                name = "red delicious"
            
            [[fruits.varieties.inside]]
                name = "granny smith"
        """.trimIndent()

        val parsedToml = tomlParser.parseString(string)
        parsedToml.prettyPrint()

        assertTrue { false }
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
