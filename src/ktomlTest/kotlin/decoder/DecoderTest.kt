package com.akuleshov7.ktoml.test.decoder

import com.akuleshov7.ktoml.decoders.DecoderConf
import com.akuleshov7.ktoml.deserialize
import com.akuleshov7.ktoml.exceptions.InvalidEnumValueException
import com.akuleshov7.ktoml.exceptions.MissingRequiredFieldException
import com.akuleshov7.ktoml.exceptions.UnknownNameDecodingException
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalSerializationApi::class)
class DecoderTest {
    enum class TestEnum {
        A, B
    }

    @Serializable
    data class SimpleTomlCase(val table1: Table1)

    @Serializable
    data class Table1(val a: Int, val b: Int)

    @Serializable
    data class Table2(val c: Int, val e: Int, val d: Int)

    @Serializable
    data class Table3(val a: Boolean, val e: String = "default", val d: Int, val b: TestEnum)

    @Serializable
    data class Table4(val c: Int, val e: Int, val d: Int, val table1: Table1)

    @Serializable
    data class ComplexPlainTomlCase(val table3: Table3)

    @Serializable
    data class TwoTomlTables(val table1: Table1, val table2: Table2)

    @Serializable
    data class TwoNestedTables(val c: Int, val table1: Table1, val table4: Table4)

    @Serializable
    data class NestedSimpleTable(val c: Int, val table1: Table1)

    @Serializable
    data class NullableValues(val a: Int?, val b: Table1?)

    @Test
    fun testForSimpleTomlCase() {
        println("table1: (a:5, b:6)")
        val test = deserialize<SimpleTomlCase>("[table1]\n b = 6  \n a = 5 ")
        println(test)
        assertEquals(SimpleTomlCase(Table1(5, 6)), test)
    }

    @Test
    fun testForTwoTomlTablesCase() {
        println("table1: (b:6, a:5), table2:(c:7, d:8)")
        val test = deserialize<TwoTomlTables>(
            "[table1]\n" +
                    " b = 6  \n" +
                    " a = 5  \n " +

                    "[table2] \n" +
                    " c = 7  \n" +
                    " d = 8  \n" +
                    " e = 9 \n"
        )
        println(test)
        assertEquals(TwoTomlTables(Table1(5, 6), Table2(7, 9, 8)), test)
    }

    @Test
    fun testForComplexTypes() {
        assertFailsWith<InvalidEnumValueException> {
            println("table3: (a:true, d:5, e:\"my test\", b: H)")
            deserialize<ComplexPlainTomlCase>("[table3] \n a = true \n d = 5 \n e = \"my test\" \n b = H")
        }
    }

    @Test
    fun testForComplexTypesExceptionOnEnums() {
        println("table3: (a:true, d:5, e:\"my test\", b = A)")
        val test = deserialize<ComplexPlainTomlCase>("[table3] \n a = true \n d = 5 \n e = my test \n b = A")
        println(test)
        assertEquals(ComplexPlainTomlCase(Table3(true, "my test", 5, b = TestEnum.A)), test)
    }

    @Test
    fun testUnknownFieldInToml() {
        assertFailsWith<UnknownNameDecodingException> {
            println("table3: (a:true, d:5, e:\"my test\", b:A, c:unknown)")
            deserialize<ComplexPlainTomlCase>("[table3] \n a = true \n d = 5 \n c = unknown \n e = my test \n b = A ")
        }
    }

    @Test
    fun testForSimpleNestedTable() {
        println("c: 5, table1: (b:6, a:5)")
        val test = deserialize<NestedSimpleTable>(
            "c = 5 \n" +
                    "[table1] \n" +
                    " b = 6  \n" +
                    " a = 5  \n "
        )
        println(test)
        assertEquals(NestedSimpleTable(5, Table1(5, 6)), test)
    }

    @Test
    fun testForNestedTables() {
        println("c:5, table1: (b:6, a:5), table4:(c:7, e:9 d:8, table1: (b:6, a:5))")
        val test = deserialize<TwoNestedTables>(
            "c = 5 \n" +
                    "[table1] \n" +
                    " b = 6  \n" +
                    " a = 5  \n " +

                    "[table4] \n" +
                    " c = 7  \n" +
                    " d = 8  \n" +
                    " e = 9 \n" +
                    " [table4.table1] \n" +
                    "   b = 6  \n" +
                    "   a = 5  \n "
        )
        println(test)
        assertEquals(TwoNestedTables(c = 5, Table1(5, 6), Table4(7, 9, 8, Table1(5, 6))), test)
    }

    @Test
    fun testWithoutTables() {
        println("a:true, b:A, e: my string, d: 55")
        val test = deserialize<Table3>(
            "a = true \n" +
                    " b = A\n" +
                    " e = my string\n" +
                    " d = 55"
        )
        println(test)
        assertEquals(Table3(true, "my string", 55, TestEnum.A), test)
    }

    @Test
    fun testForQuotes() {
        println("a:true, b:A, e: my string, d: 55")
        val test = deserialize<Table3>(
            "a = true \n" +
                    " b = A\n" +
                    " e = \"my string\"\n" +
                    " d = 55"
        )
        println(test)
        assertEquals(Table3(true, "my string", 55, TestEnum.A), test)
    }

    @Test
    fun invalidAndSoMissingRequiredKeyOnRootLevel() {
        println("a:true, d:5, e:\"my test\", err: B")
        assertFailsWith<MissingRequiredFieldException>(
            "Invalid number of arguments provided for deserialization." +
                    " Missing required field <b> in the input"
        ) {
            // 'err' key is unknown, but this should not trigger an error becuase of ignoreUnknown
            // 'b' key is not provided and should trigger an error
            deserialize<Table3>(
                " a = true \n" +
                        " d = 5 \n" +
                        " e = my test \n" +
                        " err = B",
                DecoderConf(true)
            )
        }
    }

    @Test
    fun testForUnknownFieldsWithIgnoreUnknownNamesTrueConfig() {
        println("tableUNKNOWN: (a:true, d:5, e:\"my test\", b: B)")
        assertFailsWith<MissingRequiredFieldException>(
            "Invalid number of arguments provided for deserialization." +
                    " Missing required field <table3> in the input"
        ) {
            deserialize<ComplexPlainTomlCase>(
                "[tableUNKNOWN] \n" +
                        " a = true \n" +
                        " d = 5 \n" +
                        " e = my test \n" +
                        " b = B",
                DecoderConf(true)
            )
        }

        println("a:true, b:A, d: 55, t: 7777")
        // e is missing, because it has a default value
        // t - is new unknown field
        val test = deserialize<Table3>(
            " t = \"7777\" \n" +
                    "a = true \n" +
                    " b = A \n" +
                    " d = 55 \n",

            DecoderConf(true)
        )
        assertEquals(Table3(true, b = TestEnum.A, d = 55), test)


        println("a:true, b:A, t: 7777")
        // e is missing, because it has a default value
        // t - is new unknown field
        assertFailsWith<MissingRequiredFieldException>(
            "Invalid number of arguments provided for deserialization." +
                    " Missing required field <d> in the input"
        ) {
            deserialize<Table3>(
                " t = \"7777\" \n" +
                        "a = true \n" +
                        " b = A \n",

                DecoderConf(true)
            )
        }
    }

    @Test
    fun nullableFields() {
        println("a = null, b = NULL")
        val test = deserialize<NullableValues>(
            "a = null \n " +
                    "b = NULL"
        )
        println(test)
        assertEquals(NullableValues(null, null), test)
    }

    @Test
    fun testForMissingRequiredFields() {
        println("table3: (a:true, d:5, e:\"my test\", b: B)")
        assertFailsWith<MissingRequiredFieldException>(
            "Invalid number of arguments provided for deserialization." +
                    " Missing required field <d> in the input"
        ) {
            deserialize<ComplexPlainTomlCase>(
                "[table3] \n a = true",
                DecoderConf(true)
            )
        }
    }

    @Test
    fun testForMissingRequiredTable() {
        println("table1: (a:5, b:6))")
        assertFailsWith<MissingRequiredFieldException>(
            "Invalid number of arguments provided for deserialization." +
                    " Missing required field <table2> in the input"
        ) {
            deserialize<TwoTomlTables>(
                "[table1] \n a = 5 \n b = 6",
                DecoderConf(true)
            )
        }
    }

    @Test
    fun testForMissingRequiredFieldWithDefaultValue() {
        // e - has default value and is missing in the input
        println("table3: (a:true, d:5, b: B)")
        val test = deserialize<ComplexPlainTomlCase>(
            "[table3] \n a = true \n b = B \n d = 5",
            DecoderConf(true)
        )

        println(test)
        assertEquals(ComplexPlainTomlCase(Table3(a = true, d = 5, b = TestEnum.B)), test)
    }
}
