package com.akuleshov7.ktoml.test.decoder

import com.akuleshov7.ktoml.KtomlConf
import com.akuleshov7.ktoml.deserializeToml
import com.akuleshov7.ktoml.exceptions.InvalidEnumValueException
import com.akuleshov7.ktoml.exceptions.MissingRequiredFieldException
import com.akuleshov7.ktoml.exceptions.UnknownNameDecodingException
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@ExperimentalSerializationApi
class GeneralDecoderTest {
    enum class TestEnum {
        A, B
    }

    @Serializable
    data class Regression<T>(val general: T)

    @Serializable
    data class General(val execCmd: String?)

    @Serializable
    data class GeneralInt(val execCmd: Int)

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
    data class NullableValues(
        val a: Int?, val b: Table1?, val c: String?,
        val d: String?, val e: String?, val f: String?
    )

    @Serializable
    data class ChildTableBeforeParent(val a: A)

    @Serializable
    data class A(val b: B, val a: Boolean)

    @Serializable
    data class B(val c: Int)

    @Test
    fun testForSimpleTomlCase() {
        println("table1: (a:5, b:6)")
        val test = "[table1]\n b = 6  \n a = 5 ".deserializeToml<SimpleTomlCase>()
        println(test)
        assertEquals(SimpleTomlCase(Table1(5, 6)), test)
    }

    @Test
    fun testForTwoTomlTablesCase() {
        println("table1: (b:6, a:5), table2:(c:7, d:8)")
        val test = ("[table1]\n" +
                " b = 6  \n" +
                " a = 5  \n " +

                "[table2] \n" +
                " c = 7  \n" +
                " d = 8  \n" +
                " e = 9 \n")
            .deserializeToml<TwoTomlTables>()
        println(test)
        assertEquals(TwoTomlTables(Table1(5, 6), Table2(7, 9, 8)), test)
    }

    @Test
    fun testForComplexTypes() {
        assertFailsWith<InvalidEnumValueException> {
            println("table3: (a:true, d:5, e:\"my test\", b: H)")
            "[table3] \n a = true \n d = 5 \n e = \"my test\" \n b = \"H\"".deserializeToml<ComplexPlainTomlCase>()
        }
    }

    @Test
    fun testForComplexTypesExceptionOnEnums() {
        println("table3: (a:true, d:5, e:\"my test\", b = A)")
        val test =
            "[table3] \n a = true \n d = 5 \n e = \"my test\" \n b = \"A\"".deserializeToml<ComplexPlainTomlCase>()
        println(test)
        assertEquals(ComplexPlainTomlCase(Table3(true, "my test", 5, b = TestEnum.A)), test)
    }

    @Test
    fun testUnknownFieldInToml() {
        assertFailsWith<UnknownNameDecodingException> {
            println("table3: (a:true, d:5, e:\"my test\", b:A, c:unknown)")
            ("[table3] \n a = true \n d = 5 \n" +
                    " c = \"unknown\" \n e = \"my test\" \n b = \"A\" ").deserializeToml<ComplexPlainTomlCase>()
        }
    }

    @Test
    fun testForSimpleNestedTable() {
        println("c: 5, table1: (b:6, a:5)")
        val test = ("c = 5 \n" +
                "[table1] \n" +
                " b = 6  \n" +
                " a = 5  \n ").deserializeToml<NestedSimpleTable>()
        println(test)
        assertEquals(NestedSimpleTable(5, Table1(5, 6)), test)
    }

    @Test
    fun testForNestedTables() {
        println("c:5, table1: (b:6, a:5), table4:(c:7, e:9 d:8, table1: (b:6, a:5))")
        val test = ("c = 5 \n" +
                "[table1] \n" +
                " b = 6  \n" +
                " a = 5  \n " +

                "[table4] \n" +
                " c = 7  \n" +
                " d = 8  \n" +
                " e = 9 \n" +
                " [table4.table1] \n" +
                "   b = 6  \n" +
                "   a = 5  \n ")
            .deserializeToml<TwoNestedTables>()
        println(test)
        assertEquals(TwoNestedTables(c = 5, Table1(5, 6), Table4(7, 9, 8, Table1(5, 6))), test)
    }

    @Test
    fun testWithoutTables() {
        println("a:true, b:A, e: my string, d: 55")
        val test = ("a = true \n" +
                " b = \"A\"\n" +
                " e = \"my string\"\n" +
                " d = 55")
            .deserializeToml<Table3>()
        println(test)
        assertEquals(Table3(true, "my string", 55, TestEnum.A), test)
    }

    @Test
    fun testForQuotes() {
        println("a:true, b:A, e: my string, d: 55")
        val test = ("a = true \n" +
                " b = \"A\"\n" +
                " e = \"my string\"\n" +
                " d = 55").deserializeToml<Table3>()
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
            (" a = true \n" +
                    " d = 5 \n" +
                    " e = \"my test\"\n" +
                    " err = \"B\"").deserializeToml<Table3>(
                KtomlConf(true)
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
            ("[tableUNKNOWN] \n" +
                    " a = true \n" +
                    " d = 5 \n" +
                    " e = \"my test\" \n" +
                    " b = \"B\"").deserializeToml<ComplexPlainTomlCase>(
                KtomlConf(true)
            )
        }

        println("a:true, b:A, d: 55, t: 7777")
        // e is missing, because it has a default value
        // t - is new unknown field
        val test = (" t = \"7777\" \n" +
                "a = true \n" +
                " b = \"A\" \n" +
                " d = 55 \n").deserializeToml<Table3>(
            KtomlConf(true)
        )
        assertEquals(Table3(true, b = TestEnum.A, d = 55), test)


        println("a:true, b:A, t: 7777")
        // e is missing, because it has a default value
        // t - is new unknown field
        assertFailsWith<MissingRequiredFieldException>(
            "Invalid number of arguments provided for deserialization." +
                    " Missing required field <d> in the input"
        ) {
            (" t = \"7777\" \n" +
                    "a = true \n" +
                    " b = \"A\" \n").deserializeToml<Table3>(
                KtomlConf(true)
            )
        }
    }

    @Test
    fun nullableFields() {
        println("a = null, b = NULL, c = nil")
        val test = ("a = null \n " +
                "b = NULL \n" +
                "c = nil \n" +
                "d = # hi \n" +
                "e = \n" +
                "f = NIL\n")
            .deserializeToml<NullableValues>()
        println(test)
        assertEquals(NullableValues(null, null, null, null, null, null), test)
    }

    @Test
    fun testForMissingRequiredFields() {
        println("table3: (a:true, d:5, e:\"my test\", b: B)")
        assertFailsWith<MissingRequiredFieldException>(
            "Invalid number of arguments provided for deserialization." +
                    " Missing required field <d> in the input"
        ) {
            "[table3] \n a = true".deserializeToml<ComplexPlainTomlCase>(
                KtomlConf(true)
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
            "[table1] \n a = 5 \n b = 6".deserializeToml<TwoTomlTables>(
                KtomlConf(true)
            )
        }
    }

    @Test
    fun testForMissingRequiredFieldWithDefaultValue() {
        // e - has default value and is missing in the input
        println("table3: (a:true, d:5, b: B)")
        val test = "[table3] \n a = true \n b = \"B\" \n d = 5".deserializeToml<ComplexPlainTomlCase>(

            KtomlConf(true)
        )

        println(test)
        assertEquals(ComplexPlainTomlCase(Table3(a = true, d = 5, b = TestEnum.B)), test)
    }

    @Test
    fun testChildTableBeforeParent() {
        val test = """
                |[a.b] 
                |  c = 5
                |  [a]
                |      a = true
            """.trimMargin().deserializeToml<ChildTableBeforeParent>(
            KtomlConf(true)
        )
        println(test)
        assertEquals(ChildTableBeforeParent(A(B(5), true)), test)
    }

    @Test
    fun testIncorrectEnumValue() {
        println("a:true, b:F, e: my string, d: 55")
        assertFailsWith<InvalidEnumValueException> {
            ("a = true \n" +
                    " b = \"F\"\n" +
                    " e = \"my string\"\n" +
                    " d = 55").deserializeToml<Table3>()
        }
    }

    @Test
    fun kotlinRegressionTest() {
        // this test is NOT failing on JVM but fails on mingw64 with 39 SYMBOLS and NOT failing with 38
        val test = ("[general] \n" +
                "execCmd = \"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"")
            .deserializeToml<Regression<General>>()

        assertEquals(
            Regression(
                General("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
            ),
            test
        )
    }

    @Test
    fun commentParsingTest() {
        assertEquals(
            Regression(General("dgfdgfd # f # hi")),
            """
            [general]
            execCmd = "dgfdgfd # f # hi"
        """.trimIndent().deserializeToml<Regression<General>>(),
        )

        assertEquals(
            Regression(General(null)),

            """
            [general]
            execCmd = # hello
        """.trimIndent().deserializeToml<Regression<General>>(),
        )

        assertEquals(
            Regression(General(" hello ")),

            """
            [general]
            execCmd = " hello " # hello
        """.trimIndent().deserializeToml<Regression<General>>(),
        )

        assertEquals(
            Regression(GeneralInt(0)),

            """
            [general]
            execCmd = 0 # hello
        """.trimIndent().deserializeToml<Regression<GeneralInt>>(),
        )
    }
}
