package com.akuleshov7.ktoml.encoders

import kotlinx.serialization.Serializable
import kotlin.test.Test

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
