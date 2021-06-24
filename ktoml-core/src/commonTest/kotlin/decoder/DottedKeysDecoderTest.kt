package decoder

import com.akuleshov7.ktoml.decoders.DecoderConf
import com.akuleshov7.ktoml.deserialize
import com.akuleshov7.ktoml.test.decoder.GeneralDecoderTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.test.Test

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
        val e: Int
    )

    @Serializable
    data class Table2(
        val b: B,
        val table2: InnerTable2in2
    )

    @Serializable
    data class B(
        val f: Int,
        val d: Int,
    )

    @Serializable
    data class C(
        val d: Int
    )

    @Serializable
    data class Table1(
        val a: AC,
        val table2: InnerTable2in1
    )

    @Serializable
    data class AC(
        val c: Int
    )

    @Serializable
    data class BA(
        val b: A
    )

    @Serializable
    data class A(
        val a: Int
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
        val notRequiredFieldBecauseOfEmptyTable: Int = 0
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
        deserialize<TestExample>(
            """
                      |table2.b.d = 2
                      |[table1] 
                      |a.c = 1 
                      |[table1.table2]
                      |b.b.a = 1
                      |[table2] 
                      |b.f = 2 
                      |table2."foo bar".d = 2
                      |[table3]
                      """.trimMargin(),
            DecoderConf(true)
        )
    }

    @Test
    fun tableTest() {
        deserialize<SimpleNestedExample>(
            """
                      |table2.b.d = 2
                      |[table2]
                      |e = 5
                      |b.f = 2
                      """.trimMargin(),
            DecoderConf(true)
        )

    }

    @Test
    fun tableAndDottedKeys() {
        deserialize<SimpleNestedExample>(
            """
                      |[table2]
                      |table2."foo bar".d = 2
                      |e = 6
                      |[table2.b]
                      |d = 2
                      |f = 7
                      """.trimMargin(),
            DecoderConf(true)
        )
    }
}
