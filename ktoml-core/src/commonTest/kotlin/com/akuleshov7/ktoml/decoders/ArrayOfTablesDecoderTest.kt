package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.Toml
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals

class ArrayOfTablesDecoderTest {

    @Test
    fun decodeSimpleArrayOfTables() {
        @Serializable
        data class Bar(val v: Int)
        @Serializable
        data class Foo(val x: List<Bar>)

        val toml = """
            [[x]]
                v = 1
            [[x]]
                v = 2
            [[x]]
                v = 3
        """.trimIndent()
        assertEquals(
            Foo(listOf(Bar(1), Bar(2), Bar(3))),
            Toml.decodeFromString<Foo>(toml),
        )
    }

    @Test
    fun decodeArrayOfTables() {
        @Serializable
        data class Product(
            val name: String = "name",
            val sku: Int = 1,
            val color: String? = null,
        )
        @Serializable
        data class Products(
            val products: List<Product>
        )

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

        assertEquals(
            Products(
                products = listOf(
                    Product(name = "Hammer", sku = 738594937),
                    Product(),
                    Product(name = "Nail", sku = 284758393, color = "gray"),
                )
            ),
            Toml.decodeFromString<Products>(toml),
        )
    }

    @Test
    fun decodeDottedArrayOfTables() {
        @Serializable
        data class SubElement(
            val name: String,
            val description: String,
        )
        @Serializable
        data class Element(
            val name: String,
            val subElements: List<SubElement>
        )
        @Serializable
        data class Wrapper(
            val elements: List<Element>
        )

        val toml = """
            [[elements]]
                name = "element 1"

            [[elements.subElements]]
                name = "1.1"
                description = "d1.1"

            [[elements.subElements]]
                name = "1.2"
                description = "d1.2"
        """.trimIndent()
        assertEquals(
            Wrapper(
                elements = listOf(
                    Element(
                        name = "element 1",
                        subElements = listOf(
                            SubElement("1.1", "d1.1"),
                            SubElement("1.2", "d1.2"),
                        )
                    ),
                )
            ),
            Toml.decodeFromString<Wrapper>(toml),
        )
    }

    @Test
    fun decodeDottedArrayOfTablesAndSubTable() {
        @Serializable
        data class Variety(
            val name: String
        )
        @Serializable
        data class Physical(
            val color: String,
            val shape: String
        )
        @Serializable
        data class Fruit(
            val name: String,
            val physical: Physical? = null,
            val varieties: List<Variety>
        )
        @Serializable
        data class Fruits(
            val fruits: List<Fruit>
        )

        val toml = """
            [[fruits]]
            name = "apple"

            [fruits.physical]  # subtable
            color = "red"
            shape = "round"

            [[fruits.varieties]]  # nested array of tables
            name = "red delicious"

            [[fruits.varieties]]
            name = "granny smith"

            [[fruits]]
            name = "banana"

            [[fruits.varieties]]
            name = "plantain"
        """.trimIndent()

        val firstFruit = Fruit(
            name = "apple",
            physical = Physical(color = "red", shape = "round"),
            varieties = listOf(
                Variety(name = "red delicious"),
                Variety(name = "granny smith"),
            ),
        )
        val secondFruit = Fruit(
            name = "banana",
            varieties = listOf(
                Variety(name = "plantain"),
            ),
        )
        assertEquals(
            Fruits(
                fruits = listOf(firstFruit, secondFruit)
            ),
            Toml.decodeFromString<Fruits>(toml),
        )
    }

    @Test
    fun decodeWithInlineArrayOfTablesInside() {
        @Serializable
        data class Option(
            val name: String,
            val description: String,
        )
        @Serializable
        data class Command(
            val name: String,
            val description: String,
            val options: List<Option>? = null,
        )
        @Serializable
        data class Commands(
            val commands: List<Command>,
        )

        val toml = """
            [[commands]]
	            name = "ping"
	            description = "description"
	            options = [{ name = "user", description = "descr" }]
        """.trimIndent()

        assertEquals(
            Commands(
                commands = listOf(
                    Command(
                        name = "ping",
                        description = "description",
                        options = listOf(Option(name = "user", description = "descr"))
                    )
                )
            ),
            Toml.decodeFromString<Commands>(toml),
        )
    }
}
