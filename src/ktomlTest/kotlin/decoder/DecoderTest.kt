package com.akuleshov7.ktoml.test.decoder

import com.akuleshov7.ktoml.deserialize
import com.akuleshov7.ktoml.exceptions.InvalidEnumValueException
import com.akuleshov7.ktoml.exceptions.UnknownNameDecodingException
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalSerializationApi::class)
class DecoderTest {
    enum class TestEnum {
        A, B, C
    }

    @Serializable
    data class SimpleTomlCase(val table1: Table1)

    @Serializable
    data class Table1(val a: Int, val b: Int)

    @Serializable
    data class Table2(val c: Int, val e: Int, val d: Int)

    @Serializable
    data class Table3(val a: Boolean, val e: String, val d: Int, val b: TestEnum)

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

    @Test
    fun testForSimpleTomlCase() {
        println("table1: (a:5, b:6)")
        val test = deserialize<SimpleTomlCase>("[table1]\n b = 6  \n a = 5 ")
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
        assertEquals(Table3(true, "my string", 55, TestEnum.A), test)
    }
}
