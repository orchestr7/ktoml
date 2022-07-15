package com.akuleshov7.ktoml.encoders

import com.akuleshov7.ktoml.Toml
import kotlinx.serialization.*
import kotlinx.serialization.EncodeDefault.Mode.NEVER
import kotlin.test.Test
import kotlin.test.assertEquals

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
                        Table("Plantain")
                    )
        )

        assertEquals(
            """
            [[fruits]]
            a = "apple"
            b = "qwerty"

            [[fruits]]
            a = "banana"
            b = "qwerty"

            [[fruits]]
            a = "plantain"
            b = "qwerty"
            """.trimIndent(),
            Toml.encodeToString(SimpleTableArray())
        )
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun simpleTableArrayWithEmptyTest() {
        @Serializable
        data class Table(
            @EncodeDefault(NEVER) val name: String? = null,
            @EncodeDefault(NEVER) val sku: Long? = null,
            @EncodeDefault(NEVER) val color: String? = null
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

        assertEquals(
            """
            [[products]]
                name = "Hammer"
                sku = 738594937
        
            [[products]]
        
            [[products]]
                name = "Nail"
                sku = 284758393
                color = "gray"
            """.trimIndent(),
            Toml.encodeToString(SimpleTableArrayWithEmpty())
        )
    }

    @Test
    fun nestedTableArrayTest() {
        @Serializable
        data class InnerTable(val name: String = "granny smith")

        @Serializable
        data class Table(
            val name: String = "red delicious",
            val inside: List<InnerTable> = listOf(InnerTable())
        )

        @Serializable
        data class NestedTableArray(
            @SerialName("fruits.varieties") // Todo: Not consistent with decoder behavior, but needed
            val fruitVarieties: List<Table> = listOf(Table())
        )

        assertEquals(
            """
            [[fruits.varieties]]
                name = "red delicious"
            
                [[fruits.varieties.inside]]
                    name = "granny smith"
            """.trimIndent(),
            Toml.encodeToString(NestedTableArray())
        )
    }

    @Test
    fun tableArrayWithNestedTableTest() {
        @Serializable
        data class InnerTable(val name: String = "granny smith")

        @Serializable
        data class Table(
            val name: String = "red delicious",
            val inside: InnerTable = InnerTable()
        )

        @Serializable
        data class TableArrayWithNestedTable(
            @SerialName("fruits.varieties")
            val fruitVarieties: List<Table> = listOf(Table())
        )

        assertEquals(
            """
            [[fruits.varieties]]
                name = "red delicious"
            
                [fruits.varieties.inside]
                    name = "granny smith"
            """.trimIndent(),
            Toml.encodeToString(TableArrayWithNestedTable())
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

        assertEquals(
            """
            [[fruit]]
                [fruit.physical]
                    color = "red"
                    shape = "round"
            
            [[fruit]]
                [fruit.physical]
                    color = "red"
                    shape = "round"
            """.trimIndent(),
            Toml.encodeToString(TableArrayWithNestedTable())
        )
    }

    @Test
    fun dottedFlatTableArrayTest() {
        @Serializable
        data class Table(val name: String)

        @Serializable
        data class FlatTableArray(
            @SerialName("fruits.varieties")
            val fruitVarieties: List<Table> =
                    listOf(
                        Table("red delicious"),
                        Table("granny smith"),
                        Table("granny smith"),
                    )
        )

        assertEquals(
            """
            [[fruits.varieties]]
                name = "red delicious"
            
            [[fruits.varieties]]
                name = "granny smith"
            
            [[fruits.varieties]]
                name = "granny smith"
            """.trimIndent(),
            Toml.encodeToString(FlatTableArray())
        )
    }

    @Test
    fun complexTableArrayTest1() {
        @Serializable
        data class InnerTable(val name: Long)

        @Serializable
        data class Table(
            val name: Long,
            val c: InnerTable
        )

        @Serializable
        data class ComplexTableArrays(
            @SerialName("a.b")
            val ab: List<Table> =
                    listOf(
                        Table(1, InnerTable(2)),
                        Table(3, InnerTable(4)),
                    ),
            val c: InnerTable = InnerTable(5)
        )

        assertEquals(
            """
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
            """.trimIndent(),
            Toml.encodeToString(ComplexTableArrays())
        )
    }

    @Test
    fun complexTableArrayTest2() {
        @Serializable
        data class InnerTable(val name: Long)

        @Serializable
        data class Table(
            val name: Long = 1,
            @SerialName("b.c")
            val bc: List<InnerTable> =
                    listOf(
                        InnerTable(2),
                        InnerTable(4)
                    )
        )

        @Serializable
        data class ComplexTable(val a: Table = Table())

        assertEquals(
            """
            [a]
                name = 1
            
                [[a.b.c]]
                    name = 2
            
                [[a.b.c]]
                    name = 4
            """.trimIndent(),
            Toml.encodeToString(ComplexTable())
        )
    }
}
