package com.akulashov7.ktoml.decoders

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.exceptions.InvalidEnumValueException
import com.akuleshov7.ktoml.exceptions.MissingRequiredPropertyException
import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.exceptions.UnknownNameException
import kotlinx.serialization.decodeFromString
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
    data class GeneralInt(val execCmd: Long)

    @Serializable
    data class SimpleTomlCase(val table1: Table1)

    @Serializable
    data class Table1(val a: Long, val b: Long)

    @Serializable
    data class Table2(val c: Long, val e: Long, val d: Long)

    @Serializable
    data class Table3(val a: Boolean, val e: String = "default", val d: Long, val b: TestEnum)

    @Serializable
    data class Table4(val c: Long, val e: Long, val d: Long, val table1: Table1)

    @Serializable
    data class ComplexPlainTomlCase(val table3: Table3)

    @Serializable
    data class TwoTomlTables(val table1: Table1, val table2: Table2)

    @Serializable
    data class TwoNestedTables(val c: Long, val table1: Table1, val table4: Table4)

    @Serializable
    data class NestedSimpleTable(val c: Long, val table1: Table1)

    @Serializable
    data class NullableValues(
        val a: Long?, val b: Table1?, val c: String?,
        val d: String?, val e: String?, val f: String?
    )

    @Serializable
    data class ChildTableBeforeParent(val a: A)

    @Serializable
    data class A(val b: B, val a: Boolean)

    @Serializable
    data class B(val c: Long)

    @Test
    fun testForSimpleTomlCase() {
        val test = "[table1]\n b = 6  \n a = 5 "
        assertEquals(SimpleTomlCase(Table1(5, 6)), Toml.decodeFromString(test))
    }

    @Test
    fun testForSimpleTomlCaseWithIssueInParsing() {
        val test = "[table1]\n b = 6 = 7  \n a = 5 "
        assertFailsWith<ParseException> { Toml.decodeFromString(test) }
    }

    @Test
    fun testForTwoTomlTablesCase() {
        val test = ("[table1]\n" +
                " b = 6  \n" +
                " a = 5  \n " +

                "[table2] \n" +
                " c = 7  \n" +
                " d = 8  \n" +
                " e = 9 \n")

        assertEquals(TwoTomlTables(Table1(5, 6), Table2(7, 9, 8)), Toml.decodeFromString(test))
    }

    @Test
    fun testForComplexTypes() {
        assertFailsWith<InvalidEnumValueException> {
            Toml.decodeFromString<ComplexPlainTomlCase>("[table3] \n a = true \n d = 5 \n e = \"my test\" \n b = \"H\"")
        }
    }

    @Test
    fun testForComplexTypesExceptionOnEnums() {
        val test =
            "[table3] \n a = true \n d = 5 \n e = \"my test\" \n b = \"A\""
        assertEquals(ComplexPlainTomlCase(Table3(true, "my test", 5, b = TestEnum.A)), Toml.decodeFromString(test))
    }

    @Test
    fun testUnknownFieldInToml() {
        assertFailsWith<UnknownNameException> {
            Toml.decodeFromString<ComplexPlainTomlCase>(
                "[table3] \n a = true \n d = 5 \n" +
                        " c = \"unknown\" \n e = \"my test\" \n b = \"A\" "
            )
        }
    }

    @Test
    fun testForSimpleNestedTable() {
        val test = ("c = 5 \n" +
                "[table1] \n" +
                " b = 6  \n" +
                " a = 5  \n ")
        assertEquals(NestedSimpleTable(5, Table1(5, 6)), Toml.decodeFromString(test))
    }

    @Test
    fun testForNestedTables() {
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

        assertEquals(TwoNestedTables(c = 5, Table1(5, 6), Table4(7, 9, 8, Table1(5, 6))), Toml.decodeFromString(test))
    }

    @Test
    fun testWithoutTables() {
        val test = ("a = true \n" +
                " b = \"A\"\n" +
                " e = \"my string\"\n" +
                " d = 55")

        assertEquals(Table3(true, "my string", 55, TestEnum.A), Toml.decodeFromString(test))
    }

    @Test
    fun testForQuotes() {
        val test = ("a = true \n" +
                " b = \"A\"\n" +
                " e = \"my string\"\n" +
                " d = 55")

        assertEquals(Table3(true, "my string", 55, TestEnum.A), Toml.decodeFromString(test))
    }

    @Test
    fun invalidAndSoMissingRequiredKeyOnRootLevel() {
        assertFailsWith<MissingRequiredPropertyException>(
            "Invalid number of arguments provided for deserialization." +
                    " Missing required field <b> in the input"
        ) {
            // 'err' key is unknown, but this should not trigger an error becuase of ignoreUnknown
            // 'b' key is not provided and should trigger an error
            Toml(TomlConfig(true)).decodeFromString<Table3>(
                " a = true \n" +
                        " d = 5 \n" +
                        " e = \"my test\"\n" +
                        " err = \"B\""
            )
        }
    }

    @Test
    fun testForUnknownFieldsWithIgnoreUnknownNamesTrueConfig() {
        assertFailsWith<MissingRequiredPropertyException>(
            "Invalid number of arguments provided for deserialization." +
                    " Missing required field <table3> in the input"
        ) {
            Toml(TomlConfig(true)).decodeFromString<ComplexPlainTomlCase>(
                "[tableUNKNOWN] \n" +
                        " a = true \n" +
                        " d = 5 \n" +
                        " e = \"my test\" \n" +
                        " b = \"B\""
            )
        }

        // e is missing, because it has a default value
        // t - is new unknown field
        val test = (" t = \"7777\" \n" +
                "a = true \n" +
                " b = \"A\" \n" +
                " d = 55 \n")

        assertEquals(Table3(true, b = TestEnum.A, d = 55), Toml(TomlConfig(true)).decodeFromString(test))


        // e is missing, because it has a default value
        // t - is new unknown field
        assertFailsWith<MissingRequiredPropertyException>(
            "Invalid number of arguments provided for deserialization." +
                    " Missing required field <d> in the input"
        ) {
            val failMissing = " t = \"7777\" \n" +
                    "a = true \n" +
                    " b = \"A\" \n"

            Toml(TomlConfig(true)).decodeFromString<Table3>(failMissing)
        }
    }

    @Test
    fun nullableFields() {
        val test = ("a = null \n " +
                "b = NULL \n" +
                "c = nil \n" +
                "d = # hi \n" +
                "e = \n" +
                "f = NIL\n")

        assertEquals(NullableValues(null, null, null, null, null, null), Toml.decodeFromString(test))
    }

    @Test
    fun testForMissingRequiredFields() {
        assertFailsWith<MissingRequiredPropertyException>(
            "Invalid number of arguments provided for deserialization." +
                    " Missing required field <d> in the input"
        ) {
            Toml(TomlConfig(true)).decodeFromString<Table3>("[table3] \n a = true")
        }
    }

    @Test
    fun testForMissingRequiredTable() {
        assertFailsWith<MissingRequiredPropertyException>(
            "Invalid number of arguments provided for deserialization." +
                    " Missing required field <table2> in the input"
        ) {
            Toml(TomlConfig(true)).decodeFromString<Table3>(
                "[table1] \n a = 5 \n b = 6"
            )
        }
    }

    @Test
    fun testForMissingRequiredFieldWithDefaultValue() {
        // e - has default value and is missing in the input
        val test = "[table3] \n a = true \n b = \"B\" \n d = 5"

        assertEquals(
            ComplexPlainTomlCase(Table3(a = true, d = 5, b = TestEnum.B)), Toml(
                TomlConfig(true)
            ).decodeFromString(test)
        )
    }

    @Test
    fun testChildTableBeforeParent() {
        val test = """
                |[a.b] 
                |  c = 5
                |  [a]
                |      a = true
            """.trimMargin()
        TomlConfig(true)

        assertEquals(ChildTableBeforeParent(A(B(5), true)), Toml.decodeFromString(test))
    }

    @Test
    fun testIncorrectEnumValue() {
        assertFailsWith<InvalidEnumValueException> {
            Toml(TomlConfig(true)).decodeFromString<Table3>(
                ("a = true \n" +
                        " b = \"F\"\n" +
                        " e = \"my string\"\n" +
                        " d = 55")
            )
        }
    }

    @Test
    fun kotlinRegressionTest() {
        // this test is NOT failing on JVM but fails on mingw64 with 39 SYMBOLS and NOT failing with 38
        val test = ("[general] \n" +
                "execCmd = \"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"")

        assertEquals(
            Regression(
                General("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
            ),
            Toml().decodeFromString(test)
        )
    }

    @Test
    fun commentParsingTest() {
        var test = """
            [general]
            execCmd = "dgfdgfd # f # hi"
        """.trimIndent()
        assertEquals(
            Regression(General("dgfdgfd # f # hi")),
            Toml().decodeFromString(test)
        )

        test = """
            [general]
            execCmd = # hello
        """.trimIndent()

        assertEquals(
            Regression(General(null)),

            Toml().decodeFromString(test)
        )

        test = """
            [general]
            execCmd = " hello " # hello
        """.trimIndent()
        assertEquals(
            Regression(General(" hello ")),

            Toml().decodeFromString(test)
        )

        test = """
            [general]
            execCmd = 0 # hello
        """.trimIndent()
        assertEquals(
            Regression(GeneralInt(0)),

            Toml().decodeFromString(test)
        )
    }

    @Serializable
    data class MyTest(val table: Table)

    @Serializable
    data class Table(val in1: Inner, val in2: Inner)

    @Serializable
    data class Inner(
        val a: Long,
        val in1: InnerInner,
        val in2: InnerInner
    )

    @Serializable
    data class InnerInner(val a: Long)

    @Test
    fun severalTablesOnTheSameLevel() {
        val test = """|[table]
           |[table.in1]
           |    a = 1
           |    [table.in1.in1]
           |        a = 1
           |    [table.in1.in2]
           |        a = 1
           |[table.in2]
           |    a = 1
           |    [table.in2.in1]
           |        a = 1
           |    [table.in2.in2]
           |        a = 1
        """.trimMargin()

        assertEquals(
            MyTest(
                table = Table(
                    in1 = Inner(a = 1, in1 = InnerInner(a = 1), in2 = InnerInner(a = 1)),
                    in2 = Inner(a = 1, in1 = InnerInner(a = 1), in2 = InnerInner(a = 1))
                )
            ), Toml.decodeFromString(test)
        )
    }
}
