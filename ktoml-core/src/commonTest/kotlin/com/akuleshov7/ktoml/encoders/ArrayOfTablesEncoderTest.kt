package com.akuleshov7.ktoml.encoders

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.exceptions.TomlWritingException
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlinx.serialization.encodeToString
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ArrayOfTablesEncoderTest {
    @Test
    fun simpleTableArrayTest() {
        @Serializable
        data class Table(val a: String, val b: String = "qwerty")

        @Serializable
        data class SimpleTableArray(
            val fruits: List<Table> =
                listOf(
                    Table("apple"),
                    Table("banana"),
                    Table("plantain")
                )
        )

        assertEncodedEquals(
            value = SimpleTableArray(),
            expectedToml = """
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
        )
    }

    @Test
    // reported in #321
    fun encodeArrayOfTablesNegative1() {
        @Serializable
        data class Reproducer(val foo: String, val bar: String)

        val list = listOf(
            Reproducer("foo", "bar"),
            Reproducer("bar", "baz")
        )

        assertFailsWith<TomlWritingException> {
            Toml.encodeToString(list)
        }
    }

    @Test
    // reported in #321
    fun encodeArrayOfTablesNegative2() {
        val str = "Hello world"
        assertFailsWith<TomlWritingException> {
            Toml.encodeToString(str)
        }
    }

    @Test
    // reported in #321
    fun encodeArrayOfTablesPositive() {
        @Serializable
        data class Reproducer(val foo: String, val bar: String)

        @Serializable
        data class Result(val list: List<Reproducer>)

        val result = Result(
            listOf(
                Reproducer("foo", "bar"),
                Reproducer("foo", "bar")
            )
        )

        val encoded = Toml.encodeToString(result)
        assertEncodedEquals(
            result,
            """
                [[list]]
                    foo = "foo"
                    bar = "bar"

                [[list]]
                    foo = "foo"
                    bar = "bar"
            """.trimIndent(),
        )

        val decoded = Toml.decodeFromString<Result>(encoded)
        assertEquals(Result(listOf(Reproducer("foo","bar"), Reproducer("foo", "bar"))), decoded)
    }

    @Test
    fun simpleTableArrayWithEmptyTest() {
        @Serializable
        data class Table(
            val name: String? = null,
            val sku: Long? = null,
            val color: String? = null
        )

        @Serializable
        data class SimpleTableArrayWithEmpty(
            val products: List<Table> =
                listOf(
                    Table("Hammer", 738594937),
                    Table(),
                    Table("Nail", 284758393, "gray")
                )
        )

        assertEncodedEquals(
            value = SimpleTableArrayWithEmpty(),
            expectedToml = """
                [[products]]
                    name = "Hammer"
                    sku = 738594937
            
                [[products]]
                
                [[products]]
                    name = "Nail"
                    sku = 284758393
                    color = "gray"
            """.trimIndent()
        )
    }

    @Test
    fun nestedTableArrayTest() {
        @Serializable
        data class InnerTable(val name: String = "granny smith")

        @Serializable
        data class Table2(
            val name: String = "red delicious",
            val inside: List<InnerTable> = listOf(InnerTable())
        )

        @Serializable
        data class Table1(
            val varieties: List<Table2> = listOf(Table2())
        )

        @Serializable
        data class NestedTableArray(
            val fruits: List<Table1> = listOf(Table1())
        )

        assertEncodedEquals(
            value = NestedTableArray(),
            expectedToml = """
                [[fruits.varieties]]
                    name = "red delicious"
                
                    [[fruits.varieties.inside]]
                        name = "granny smith"
            """.trimIndent()
        )
    }

    @Test
    fun encodeSubSubTablesWithMultipleValues() {
        @Serializable
        data class SubSubElement(
            val name: String,
            val description: String,
        )

        @Serializable
        data class SubElement(
            val name: String,
            val description: String,
            val subSubElements: List<SubSubElement>,
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


        val data = Wrapper(
            elements = listOf(
                Element(
                    name = "element 1",
                    subElements = listOf(
                        SubElement(
                            "1.1", "d1.1",
                            subSubElements =
                            listOf(
                                SubSubElement("1.1.1", "d1.1.1"),
                                SubSubElement("1.1.2", "d1.1.2")
                            )
                        ),
                        SubElement(
                            "1.2", "d1.2",
                            subSubElements =
                            listOf(
                                SubSubElement("1.2.1", "d1.2.1"),
                                SubSubElement("1.2.2", "d1.2.2"),
                            )
                        ),
                    )
                ),
                Element(
                    name = "element 2",
                    subElements = listOf(
                        SubElement(
                            "2.1", "d2.1",
                            subSubElements =
                            listOf(
                                SubSubElement("2.1.1", "d2.1.1"),
                                SubSubElement("2.1.2", "d2.1.2"),
                            )
                        ),
                        SubElement(
                            "2.2", "d2.2",
                            subSubElements =
                            listOf(
                                SubSubElement("2.2.1", "d2.2.1"),
                                SubSubElement("2.2.2", "d2.2.2"),
                            )
                        ),
                    )
                ),
            )
        )

        assertEncodedEquals(
            value = data,
            expectedToml = """
                [[elements]]
                    name = "element 1"
                
                    [[elements.subElements]]
                        name = "1.1"
                        description = "d1.1"
                
                        [[elements.subElements.subSubElements]]
                            name = "1.1.1"
                            description = "d1.1.1"
                
                        [[elements.subElements.subSubElements]]
                            name = "1.1.2"
                            description = "d1.1.2"
                
                    [[elements.subElements]]
                        name = "1.2"
                        description = "d1.2"
                
                        [[elements.subElements.subSubElements]]
                            name = "1.2.1"
                            description = "d1.2.1"
                
                        [[elements.subElements.subSubElements]]
                            name = "1.2.2"
                            description = "d1.2.2"
                
                [[elements]]
                    name = "element 2"
                
                    [[elements.subElements]]
                        name = "2.1"
                        description = "d2.1"
                
                        [[elements.subElements.subSubElements]]
                            name = "2.1.1"
                            description = "d2.1.1"
                
                        [[elements.subElements.subSubElements]]
                            name = "2.1.2"
                            description = "d2.1.2"
                
                    [[elements.subElements]]
                        name = "2.2"
                        description = "d2.2"
                
                        [[elements.subElements.subSubElements]]
                            name = "2.2.1"
                            description = "d2.2.1"
                
                        [[elements.subElements.subSubElements]]
                            name = "2.2.2"
                            description = "d2.2.2"
            """.trimIndent()
        )
    }

    @Test
    fun tableArrayWithNestedTableTest() {
        @Serializable
        data class InnerTable(val name: String = "granny smith")

        @Serializable
        data class Table2(
            val name: String = "red delicious",
            val inside: InnerTable = InnerTable()
        )

        @Serializable
        data class Table1(val varieties: List<Table2> = listOf(Table2()))

        @Serializable
        data class TableArrayWithNestedTable(
            val fruits: List<Table1> = listOf(Table1())
        )

        assertEncodedEquals(
            value = TableArrayWithNestedTable(),
            expectedToml = """
                [[fruits.varieties]]
                    name = "red delicious"
                
                    [fruits.varieties.inside]
                        name = "granny smith"
            """.trimIndent()
        )
    }

    @Test
    fun tableArrayWithNestedTableTest2() {
        @Serializable
        data class InnerTable(
            val color: String = "red",
            val shape: String = "round"
        )

        @Serializable
        data class Table(val physical: InnerTable = InnerTable())

        @Serializable
        data class TableArrayWithNestedTable(
            val fruit: List<Table> =
                listOf(
                    Table(),
                    Table()
                )
        )

        assertEncodedEquals(
            value = TableArrayWithNestedTable(),
            expectedToml = """
                [[fruit]]
                    [fruit.physical]
                        color = "red"
                        shape = "round"
                
                [[fruit]]
                    [fruit.physical]
                        color = "red"
                        shape = "round"
            """.trimIndent()
        )
    }

    @Test
    fun dottedFlatTableArrayTest() {
        @Serializable
        data class Table2(val name: String)

        @Serializable
        data class Table1(
            val varieties: List<Table2> =
                listOf(
                    Table2("red delicious"),
                    Table2("granny smith"),
                    Table2("granny smith"),
                )
        )

        @Serializable
        data class FlatTableArray(
            val fruits: List<Table1> = listOf(Table1())

        )

        assertEncodedEquals(
            value = FlatTableArray(),
            expectedToml = """
                [[fruits.varieties]]
                    name = "red delicious"
                
                [[fruits.varieties]]
                    name = "granny smith"
                
                [[fruits.varieties]]
                    name = "granny smith"
            """.trimIndent()
        )
    }

    @Test
    fun complexTableArrayTest1() {
        @Serializable
        data class InnerTable(val name: Long)

        @Serializable
        data class Table2(
            val name: Long,
            val c: InnerTable
        )

        @Serializable
        data class Table1(
            val b: List<Table2> =
                listOf(
                    Table2(1, InnerTable(2)),
                    Table2(3, InnerTable(4)),
                )
        )

        @Serializable
        data class ComplexTableArrays(
            val a: Table1 = Table1(),
            val c: List<InnerTable> = listOf(InnerTable(5))
        )

        assertEncodedEquals(
            value = ComplexTableArrays(),
            expectedToml = """
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
        )
    }

    @Test
    fun complexTableArrayTest2() {
        @Serializable
        data class InnerTable(val name: Long)

        @Serializable
        data class Table2(
            val c: List<InnerTable> =
                listOf(
                    InnerTable(2),
                    InnerTable(4)
                )
        )

        @Serializable
        data class Table1(
            val name: Long = 1,
            val b: Table2 = Table2()
        )

        @Serializable
        data class ComplexTable(val a: Table1 = Table1())

        assertEncodedEquals(
            value = ComplexTable(),
            expectedToml = """
                [a]
                    name = 1
                
                    [[a.b.c]]
                        name = 2
                
                    [[a.b.c]]
                        name = 4
            """.trimIndent()
        )
    }
}
