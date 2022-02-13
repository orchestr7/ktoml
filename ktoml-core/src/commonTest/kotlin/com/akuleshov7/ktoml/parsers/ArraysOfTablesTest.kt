package com.akuleshov7.ktoml.parsers

import com.akuleshov7.ktoml.Toml.Default.tomlParser
import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.tree.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ArraysOfTablesTest {
    @Test
    fun positiveSimpleParsing() {
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
    fun parsingOfEmptyArrayOfTables() {
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

        val arrayOfTables = parsedToml.children.last()
        assertTrue { arrayOfTables is TomlArrayOfTables }

        val children = arrayOfTables.children
        assertEquals(children.size, 3)
        children.forEach { assertTrue { it is TomlArrayOfTablesElement } }

        assertTrue { children[1].children.isEmpty() }
    }

    @Test
    fun parsingNestedArraysOfTables1() {
        val string = """
            [[fruits.varieties]] 
                name = "red delicious"
            
            [[fruits.varieties.inside]]
                name = "granny smith"
        """.trimIndent()

        val parsedToml = tomlParser.parseString(string)
        parsedToml.prettyPrint()

        val fruitVarieties = parsedToml.children.last().children.last()
        assertTrue { fruitVarieties is TomlArrayOfTables }

        val firstElement = fruitVarieties.children.last()
        assertTrue { firstElement is TomlArrayOfTablesElement }

        val lastElement = firstElement.children.last().children.last()
        assertTrue { lastElement is TomlArrayOfTablesElement }
    }

    @Test
    fun parsingNestedArraysOfTables2() {
        val string = """
            [[fruits.varieties]] 
                name = "red delicious"
            
            [fruits.varieties.inside]
                name = "granny smith"
        """.trimIndent()

        val parsedToml = tomlParser.parseString(string)
        parsedToml.prettyPrint()

        val fruitVarieties = parsedToml.children.last().children.last()
        assertTrue { fruitVarieties is TomlArrayOfTables }

        val firstElement = fruitVarieties.children.last()
        assertTrue { firstElement is TomlArrayOfTablesElement }

        val lastElement = firstElement.children.last()
        assertTrue { lastElement is TomlTablePrimitive }
    }

    @Test
    fun parsingSimpleArrayOfTables1() {
        val string = """
            [[fruits.varieties]] 
                name = "red delicious"
            
            [[fruits.varieties]]
                name = "granny smith"
            
            [[fruits.varieties]]
                name = "granny smith"
        """.trimIndent()

        val parsedToml = tomlParser.parseString(string)
        parsedToml.prettyPrint()

        val fruitVarieties = parsedToml.children.last().children.last()
        assertEquals(3, fruitVarieties.children.size)
        fruitVarieties.children.forEach {
            assertTrue { it is TomlArrayOfTablesElement }
        }
    }

    @Test
    fun parsingSimpleArrayOfTables2() {
        //    FixMe: here should throw an exception in case of table duplication https://github.com/akuleshov7/ktoml/issues/30
        val string = """
            [[fruits.varieties]] 
                name = "red delicious"
            
            [fruits.varieties]
                name = "granny smith"
                
            [a]
                b = 1
            
            [[a]]
                b = 1
        """.trimIndent()

        val parsedToml = tomlParser.parseString(string)
        parsedToml.prettyPrint()

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
    fun nestedArraysOnly1() {
        val string = """
            # bug here
            [[a]]
                name = 1
            
            [[a.b]]
                name = 2
            
            [[a]]
                name = 3
            
            [[a.b]]
                name = 4
                
            [[c]]
                name = 5
        """.trimIndent()

        val parsedToml = tomlParser.parseString(string)
        assertEquals(
            """
            | - TomlFile (rootNode)
            |     - TomlArrayOfTables ([[a]])
            |         - TomlArrayOfTablesElement (technical_node)
            |             - TomlKeyValuePrimitive (name=1)
            |             - TomlArrayOfTables ([[a.b]])
            |                 - TomlArrayOfTablesElement (technical_node)
            |                     - TomlKeyValuePrimitive (name=2)
            |         - TomlArrayOfTablesElement (technical_node)
            |             - TomlKeyValuePrimitive (name=3)
            |             - TomlArrayOfTables ([[a.b]])
            |                 - TomlArrayOfTablesElement (technical_node)
            |                     - TomlKeyValuePrimitive (name=4)
            |     - TomlArrayOfTables ([[c]])
            |         - TomlArrayOfTablesElement (technical_node)
            |             - TomlKeyValuePrimitive (name=5)
            |
        """.trimMargin(),
            parsedToml.prettyStr()
        )
    }

    @Test
    fun nestedArraysOnly2() {
        val string = """
            # bug here - нужно раскручивать до последнего НЕ синтетика
            [[a]]
                name = 1
            
            [[a.b.c]]
                name = 2
            
            [[a]]
                name = 3
            
            [[a.b.c]]
                name = 4
                
            [[a.b.c.d]]
                
            [[c]]
                name = 5
        """.trimIndent()

        val parsedToml = tomlParser.parseString(string)
        parsedToml.prettyPrint()

        assertTrue { false }
    }

    @Test
    fun parsingNestedArraysOfTablesRegression() {
        val string = """
            [[a]]
                name = 1
            
            [a.b]
                name = 2
            
            [[a]]
                name = 3
            
            [a.b]
                name = 4
                
            [[c]]
                name = 5
        """.trimIndent()

        val parsedToml = tomlParser.parseString(string)
        parsedToml.prettyPrint()

        assertEquals(
            """
                | - TomlFile (rootNode)
                |     - TomlArrayOfTables ([[a]])
                |         - TomlArrayOfTablesElement (technical_node)
                |             - TomlKeyValuePrimitive (name=1)
                |             - TomlTablePrimitive ([a.b])
                |                 - TomlKeyValuePrimitive (name=2)
                |         - TomlArrayOfTablesElement (technical_node)
                |             - TomlKeyValuePrimitive (name=3)
                |             - TomlTablePrimitive ([a.b])
                |                 - TomlKeyValuePrimitive (name=4)
                |     - TomlArrayOfTables ([[c]])
                |         - TomlArrayOfTablesElement (technical_node)
                |             - TomlKeyValuePrimitive (name=5)
                |
        """.trimMargin(),
            parsedToml.prettyStr()
        )
    }

    @Test
    fun mixedTablesAndArrayOfTables1() {
        val string = """
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


        val parsedToml = tomlParser.parseString(string)
        assertEquals(
            """
                | - TomlFile (rootNode)
                |     - TomlArrayOfTables ([[a]])
                |         - TomlArrayOfTables ([[a.b]])
                |             - TomlArrayOfTablesElement (technical_node)
                |                 - TomlKeyValuePrimitive (name=1)
                |                 - TomlTablePrimitive ([a.b.c])
                |                     - TomlKeyValuePrimitive (name=2)
                |             - TomlArrayOfTablesElement (technical_node)
                |                 - TomlKeyValuePrimitive (name=3)
                |                 - TomlTablePrimitive ([a.b.c])
                |                     - TomlKeyValuePrimitive (name=4)
                |     - TomlArrayOfTables ([[c]])
                |         - TomlArrayOfTablesElement (technical_node)
                |             - TomlKeyValuePrimitive (name=5)
                |
        """.trimMargin(),
            parsedToml.prettyStr()
        )
    }

    @Test
    fun mixedTablesAndArrayOfTables2() {
        val string = """
            [a]
                name = 1
            
            [[a.b.c]]
                name = 2
            
            [[a.b.c]]
                name = 4
        """.trimIndent()


        val parsedToml = tomlParser.parseString(string)
        parsedToml.prettyPrint()
        assertEquals(
            """
                | - TomlFile (rootNode)
                |     - TomlStubEmptyNode (technical_node)
                |     - TomlTablePrimitive ([a])
                |         - TomlKeyValuePrimitive (name=1)
                |         - TomlArrayOfTables ([[a.b]])
                |             - TomlArrayOfTables ([[a.b.c]])
                |                 - TomlArrayOfTablesElement (technical_node)
                |                     - TomlKeyValuePrimitive (name=2)
                |                 - TomlArrayOfTablesElement (technical_node)
                |                     - TomlKeyValuePrimitive (name=4)
                |
        """.trimMargin(),
            parsedToml.prettyStr()
        )
    }
}
