package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.exceptions.InvalidEnumValueException
import com.akuleshov7.ktoml.exceptions.MissingRequiredPropertyException
import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.exceptions.UnknownNameException
import io.kotest.matchers.throwable.shouldHaveMessage
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlin.test.Test

@ExperimentalSerializationApi
class GeneralDecoderTest {
    enum class TestEnum {
        A, B
    }

    @Serializable
    data class OptionalRegression(val test: String = "")

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
        "[table1]\n b = 6  \n a = 5 "
            .shouldDecodeInto(SimpleTomlCase(Table1(5, 6)))
    }

    @Test
    fun testForSimpleTomlCaseWithIssueInParsing() {
        "[table1]\n b = 6 = 7  \n a = 5 "
            .shouldThrowExceptionWhileDecoding<SimpleTomlCase, ParseException>()
    }

    @Test
    fun testForTwoTomlTablesCase() {
        ("[table1]\n" +
                " b = 6  \n" +
                " a = 5  \n " +

                "[table2] \n" +
                " c = 7  \n" +
                " d = 8  \n" +
                " e = 9 \n")
            .shouldDecodeInto(TwoTomlTables(Table1(5, 6), Table2(7, 9, 8)))
    }

    @Test
    fun testForComplexTypes() {
        "[table3] \n a = true \n d = 5 \n e = \"my test\" \n b = \"H\""
            .shouldThrowExceptionWhileDecoding<ComplexPlainTomlCase, InvalidEnumValueException>()
    }

    @Test
    fun testForComplexTypesExceptionOnEnums() {
        "[table3] \n a = true \n d = 5 \n e = \"my test\" \n b = \"A\""
            .shouldDecodeInto(ComplexPlainTomlCase(Table3(true, "my test", 5, b = TestEnum.A)))
    }

    @Test
    fun testUnknownFieldInToml() {
        "[table3] \n a = true \n d = 5 \n c = \"unknown\" \n e = \"my test\" \n b = \"A\" "
            .shouldThrowExceptionWhileDecoding<ComplexPlainTomlCase, UnknownNameException>()
    }

    @Test
    fun testForSimpleNestedTable() {
        """
            c = 5 
            [table1] 
            b = 6  
            a = 5  
        """.trimIndent()
            .shouldDecodeInto(NestedSimpleTable(5, Table1(5, 6)))
    }

    @Test
    fun testForNestedTables() {
        ("c = 5 \n" +
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
            .shouldDecodeInto(TwoNestedTables(c = 5, Table1(5, 6), Table4(7, 9, 8, Table1(5, 6))))
    }

    @Test
    fun testWithoutTables() {
        ("a = true \n" +
                " b = \"A\"\n" +
                " e = \"my string\"\n" +
                " d = 55")
            .shouldDecodeInto(Table3(true, "my string", 55, TestEnum.A))
    }

    @Test
    fun testForQuotes() {
        ("a = true \n" +
                " b = \"A\"\n" +
                " e = \"my string\"\n" +
                " d = 55")
            .shouldDecodeInto(Table3(true, "my string", 55, TestEnum.A))
    }

    @Test
    fun invalidAndSoMissingRequiredKeyOnRootLevel() {
        // 'err' key is unknown, but this should not trigger an error becuase of ignoreUnknown
        // 'b' key is not provided and should trigger an error
        (" a = true \n" +
                " d = 5 \n" +
                " e = \"my test\"\n" +
                " err = \"B\"")
            .shouldThrowExceptionWhileDecoding<Table3, MissingRequiredPropertyException>(
                tomlInstance = Toml(TomlInputConfig(true))
            )
            .shouldHaveMessage(
                "Invalid number of key-value arguments provided in the input for deserialization. " +
                        "Missing required property <b> from class <com.akuleshov7.ktoml.decoders.GeneralDecoderTest.Table3> " +
                        "in the input. (In your deserialization class you have declared this field, but it is missing in the input)"
            )
    }

    @Test
    fun testForUnknownFieldsWithIgnoreUnknownNamesTrueConfig() {
        ("[tableUNKNOWN] \n" +
                " a = true \n" +
                " d = 5 \n" +
                " e = \"my test\" \n" +
                " b = \"B\"")
            .shouldThrowExceptionWhileDecoding<ComplexPlainTomlCase, MissingRequiredPropertyException>()
            .shouldHaveMessage(
                "Invalid number of key-value arguments provided in the input for deserialization. Missing required property " +
                        "<table3> from class <com.akuleshov7.ktoml.decoders.GeneralDecoderTest.ComplexPlainTomlCase> " +
                        "in the input. (In your deserialization class you have declared this field, but it is missing " +
                        "in the input)"
            )

        // e is missing, because it has a default value
        // t - is new unknown field
        (" t = \"7777\" \n" +
                "a = true \n" +
                " b = \"A\" \n" +
                " d = 55 \n")
            .shouldDecodeInto(
                decodedValue = Table3(true, b = TestEnum.A, d = 55),
                tomlInstance = Toml(TomlInputConfig(true))
            )

        // e is missing, because it has a default value
        // t - is new unknown field
        (" t = \"7777\" \n" +
                "a = true \n" +
                " b = \"A\" \n")
            .shouldThrowExceptionWhileDecoding<Table3, MissingRequiredPropertyException>(
                tomlInstance = Toml(TomlInputConfig(true))
            )
            .shouldHaveMessage(
                "Invalid number of key-value arguments provided in the input for deserialization. Missing required " +
                        "property <d> from class <com.akuleshov7.ktoml.decoders.GeneralDecoderTest.Table3> in the input. " +
                        "(In your deserialization class you have declared this field, but it is missing in the input)"
            )
    }

    @Test
    fun nullableFields() {
        val test = ("a = null \n " +
                "b = NULL \n" +
                "c = nil \n" +
                "d = # hi \n" +
                "e = \n" +
                "f = NIL\n")

        test.shouldDecodeInto(NullableValues(null, null, null, null, null, null))
        test.shouldThrowExceptionWhileDecoding<NullableValues, ParseException>(
            tomlInstance = Toml(TomlInputConfig(allowNullValues = false))
        )
    }

    @Test
    fun testForMissingRequiredFields() {
        "[table3] \n a = true"
            .shouldThrowExceptionWhileDecoding<Table3, MissingRequiredPropertyException>()
            .shouldHaveMessage(
                "Invalid number of key-value arguments provided in the input for deserialization. Missing required " +
                        "property <a> from class <com.akuleshov7.ktoml.decoders.GeneralDecoderTest.Table3> in the input. " +
                        "(In your deserialization class you have declared this field, but it is missing in the input)"
            )
    }

    @Test
    fun testForMissingRequiredTable() {
        "[table1] \n a = 5 \n b = 6"
            .shouldThrowExceptionWhileDecoding<Table3, MissingRequiredPropertyException>()
            .shouldHaveMessage(
                "Invalid number of key-value arguments provided in the input for deserialization. " +
                        "Missing required property <a> from class <com.akuleshov7.ktoml.decoders.GeneralDecoderTest.Table3> " +
                        "in the input. (In your deserialization class you have declared this field, but it is missing " +
                        "in the input)"
            )
    }

    @Test
    fun testForMissingRequiredFieldWithDefaultValue() {
        // e - has default value and is missing in the input
        "[table3] \n a = true \n b = \"B\" \n d = 5"
            .shouldDecodeInto(
                decodedValue = ComplexPlainTomlCase(Table3(a = true, d = 5, b = TestEnum.B)),
                tomlInstance = Toml(TomlInputConfig(true))
            )
    }

    @Test
    fun testChildTableBeforeParent() {
        """
            [a.b] 
                c = 5
                [a]
                   a = true
        """.shouldDecodeInto(ChildTableBeforeParent(A(B(5), true)))
    }

    @Test
    fun testIncorrectEnumValue() {
        ("a = true \n" +
                " b = \"F\"\n" +
                " e = \"my string\"\n" +
                " d = 55")
            .shouldThrowExceptionWhileDecoding<Table3, InvalidEnumValueException>()
    }

    @Test
    fun kotlinRegressionTest() {
        // this test is NOT failing on JVM but fails on mingw64 with 39 SYMBOLS and NOT failing with 38
        ("[general] \n" + "execCmd = \"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"")
            .shouldDecodeInto(
                Regression(
                    General("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
                )
            )
    }

    @Test
    fun commentParsingTest() {
        """
            [general]
            execCmd = "dgfdgfd # f # hi"
        """.trimIndent()
            .shouldDecodeInto(Regression(General("dgfdgfd # f # hi")))

        """
            [general]
            execCmd = # hello
        """.trimIndent()
            .shouldDecodeInto(Regression(General(null)))

        """
            [general]
            execCmd = " hello " # hello
        """.trimIndent()
            .shouldDecodeInto(Regression(General(" hello ")))

        """
            [general]
            execCmd = 0 # hello
        """.trimIndent()
            .shouldDecodeInto(Regression(GeneralInt(0)))
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
        """
            [table]
            [table.in1]
                a = 1
                [table.in1.in1]
                    a = 1
                [table.in1.in2]
                    a = 1
            [table.in2]
                a = 1
                [table.in2.in1]
                    a = 1
                [table.in2.in2]
                    a = 1
        """.shouldDecodeInto(
            MyTest(
                table = Table(
                    in1 = Inner(a = 1, in1 = InnerInner(a = 1), in2 = InnerInner(a = 1)),
                    in2 = Inner(a = 1, in1 = InnerInner(a = 1), in2 = InnerInner(a = 1))
                )
            )
        )
    }


    @Test
    // this logic will be changed in https://github.com/akuleshov7/ktoml/issues/30
    fun tablesRedeclaration() {
        """
            [table1]
            a = 2

            [table1]
            a = 1
            b = 2
        """.trimIndent()
            .shouldDecodeInto(SimpleTomlCase(Table1(1, 2)))
    }

    @Test
    fun optionalEmptyStringRegression() {
        """
        """.trimIndent()
            .shouldDecodeInto(OptionalRegression(""))
    }
}
