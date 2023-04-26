package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlInputConfig
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalSerializationApi
class DottedKeysDecoderTest {
    @Serializable
    data class TestExample(
        val table1: Table1,
        val table2: Table2,
        val table3: Table3
    )

    @Serializable
    data class SimpleNestedExample(
        val table2: Table4
    )

    @Serializable
    data class Table4(
        val b: B,
        val e: Long
    )

    @Serializable
    data class Table2(
        val b: B,
        val table2: InnerTable2in2
    )

    @Serializable
    data class B(
        val f: Long,
        val d: Long,
    )

    @Serializable
    data class C(
        val d: Long
    )

    @Serializable
    data class Table1(
        val a: AC,
        val table2: InnerTable2in1
    )

    @Serializable
    data class AC(
        val c: Long
    )

    @Serializable
    data class BA(
        val b: A
    )

    @Serializable
    data class A(
        val a: Long
    )

    @Serializable
    data class InnerTable2in1(
        val b: BA
    )

    @Serializable
    data class InnerTable2in2(
        @SerialName("foo bar")
        val myFieldWithSerialName: C
    )

    @Serializable
    data class Table3(
        val notRequiredFieldBecauseOfEmptyTable: Long = 0
    )


    /**
     * table2.b.d = 2
     *
     * [table1]
     *     a.c = 1
     * [table1.table2]
     *     b.a = 1
     * [table2]
     *     b.f = 2
     * # even though the next key look to be on upper-level, it belongs to table2
     * table2."foo bar".c = 2
     *
     * [table3]
     * # empty table
     */
    @ExperimentalSerializationApi
    @Test
    fun testDottedKeys() {
        assertEquals(
            TestExample(
                table1 = Table1(a = AC(c = 1), table2 = InnerTable2in1(b = BA(b = A(a = 1)))),
                table2 = Table2(b = B(f = 2, d = 2), table2 = InnerTable2in2(myFieldWithSerialName = C(d = 2))),
                table3 = Table3(notRequiredFieldBecauseOfEmptyTable = 0)
            ),
            Toml.decodeFromString(
                """
                      table2.b.d = 2
                      [table1] 
                          a.c = 1 
                      [table1.table2]
                          b.b.a = 1
                      [table2] 
                          b.f = 2 
                          table2."foo bar".d = 2
                      [table3]
                      """
            )
        )
    }

    @Test
    fun tableTest() {
        assertEquals(
            SimpleNestedExample(table2 = Table4(b = B(f = 2, d = 2), e = 5)),
            Toml(TomlInputConfig(true)).decodeFromString(
                """
                      table2.b.d = 2
                      [table2]
                          e = 5
                          b.f = 2
                      """
            )
        )
    }

    @Test
    fun tableAndDottedKeys() {
        assertEquals(
            SimpleNestedExample(table2 = Table4(b = B(f = 7, d = 2), e = 6)),
            Toml(TomlInputConfig(true)).decodeFromString(
                """
                      [table2]
                          table2."foo bar".d = 2
                          e = 6
                      [table2.b]
                          d = 2
                          f = 7
                      """
            )
        )
    }

    @Serializable
    data class DottedTable(
        @SerialName("a")
        val a1: AClass
    )

    @Serializable
    data class AClass(
        @SerialName("b.c..")
        val bc1: BCClass,
    )

    @Serializable
    data class BCClass(
        @SerialName("val")
        val variable: Long,
        val d: DClass,
        val inner: InnerClass
    )

    @Serializable
    data class DClass(
        @SerialName("e.f")
        val ef: EFClass,
    )

    @Serializable
    data class EFClass(
        @SerialName("val")
        val variable: Long
    )

    @Serializable
    data class InnerClass(
        @SerialName("val")
        val variable: Long
    )

    @Test
    fun dottedTableDecoder() {
        assertEquals(
            DottedTable(
                a1 = AClass(
                    bc1 = BCClass(
                        variable = 2,
                        d = DClass(ef = EFClass(variable = 1)),
                        inner = InnerClass(variable = 3)
                    )
                )
            ),
            Toml.decodeFromString(
                """
            [a."b.c..".d."e.f"]
                val = 1
             [a]
             [a."b.c.."]
                val = 2
             [a."b.c..".inner]
                val = 3
        """
            )
        )
    }

    @Serializable
    data class QuotedKey(val a: AQ)

    @Serializable
    data class AQ(
        @SerialName("a.b.c")
        val b: ABCQ
    )

    @Serializable
    data class ABCQ(
        @SerialName("b")
        val b: BQ
    )

    @Serializable
    data class BQ(
        val d: Long
    )

    @Test
    fun decodeQuotedKey() {
        assertEquals(
            QuotedKey(a = AQ(b = ABCQ(b = BQ(d = 123)))),
            Toml.decodeFromString("a.\"a.b.c\".b.d = 123")
        )
    }
}
