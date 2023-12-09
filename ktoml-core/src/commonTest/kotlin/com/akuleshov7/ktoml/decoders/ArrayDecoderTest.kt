package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.exceptions.IllegalTypeException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.test.Ignore
import kotlin.test.Test

@Serializable
data class SimpleArray(val a: List<Long>)

@Serializable
data class SimpleArrayWithNullableValues(val a: List<Long?>)

@Serializable
data class SimpleStringArray(val a: List<String>)

@Serializable
data class NestedArray(val a: List<List<Long>>)

@Serializable
data class ArrayInInlineTable(val table: InlineTable)

@Serializable
data class InlineTable(
    val name: String,
    @SerialName("configurationList")
    val overriddenName: List<String>
)

@Serializable
data class TestArrays(
    @SerialName("configurationList1")
    val overriddenName1: List<String>,
    @SerialName("configurationList2")
    val overriddenName2: List<String>,
    val table: Table
)

@Serializable
data class Table(val name: String)

@Serializable
data class TestArraysAndSimple(
    val name1: String,
    @SerialName("configurationList1")
    val overriddenName1: List<String>,
    @SerialName("configurationList2")
    val overriddenName2: List<String>,
    val table: Table,
    val name2: String
)

@Serializable
data class ClassWithMutableList (val field: MutableList<Long>? = null)

@Serializable
data class ClassWithImmutableList(val field: List<Long?>? = null)

class SimpleArrayDecoderTest {
    @Test
    fun testRegressions() {
        // ==== #77 ====
        "field = []"
            .shouldDecodeInto(ClassWithMutableList(mutableListOf()))

        "field = []"
            .shouldDecodeInto(ClassWithImmutableList(emptyList()))

        "field = null"
            .shouldDecodeInto(ClassWithImmutableList(null))

        "field = null"
            .shouldDecodeInto(ClassWithMutableList(null))

        "field = [null]"
            .shouldThrowExceptionWhileDecoding<ClassWithMutableList, IllegalTypeException>()

        "field = [null ]"
            .shouldDecodeInto(ClassWithImmutableList(listOf(null)))

        "field = [null, 1 ]"
            .shouldDecodeInto(ClassWithImmutableList(listOf(null, 1)))
    }

    @Test
    fun testSimpleArrayDecoder() {
        "a = [1, 2,      3]"
            .shouldDecodeInto(SimpleArray(listOf(1, 2, 3)))
    }

    @Test
    fun testArrayWithTrailingComma() {
        "a = [1, 2, 3,]"
            .shouldDecodeInto(SimpleArrayWithNullableValues(listOf(1, 2, 3)))
    }

    @Test
    fun testSimpleArrayDecoderInNestedTable() {
        """
            [table]
                name = "my name"
                configurationList = ["a",  "b",  "c"]
        """.shouldDecodeInto(
            ArrayInInlineTable(
                table = InlineTable(
                    name = "my name",
                    overriddenName = listOf("a", "b", "c")
                )
            )
        )

        """
            [table]
                configurationList = ["a",  "b",  "c"]
                name = "my name"
        """.shouldDecodeInto(
            ArrayInInlineTable(
                table = InlineTable(
                    name = "my name",
                    overriddenName = listOf("a", "b", "c")
                )
            )
        )

        """
            configurationList1 = ["a",  "b",  "c"]
            configurationList2 = ["a",  "b",  "c"]
            [table]
                name = "my name"
        """.shouldDecodeInto(TestArrays(
                overriddenName1 = listOf("a", "b", "c"),
                overriddenName2 = listOf("a", "b", "c"),
                table = Table(name = "my name")
            )
        )

        """
            name1 = "simple"
            configurationList1 = ["a",  "b",  "c"]
            name2 = "simple"
            configurationList2 = ["a",  "b",  "c"]
            [table]
                name = "my name"
        """.shouldDecodeInto(
            TestArraysAndSimple(
                name1 = "simple",
                overriddenName1 = listOf("a", "b", "c"),
                overriddenName2 = listOf("a", "b", "c"),
                table = Table(name = "my name"),
                name2 = "simple"
            )
        )
    }

    @Test
    @Ignore
    fun testNestedArrayDecoder() {
        // FixMe: nested array decoding causes issues and is not supported yet
        "a = [[1, 2],      [3,  4]]"
            .shouldDecodeInto(NestedArray(listOf(listOf(1, 2), listOf(3, 4))))
    }

    @Test
    fun testCommasInString() {
        "a = [\"yes, indeed\", \"hmm, hmm\"]"
            .shouldDecodeInto(SimpleStringArray(listOf("yes, indeed", "hmm, hmm")))
        "a = ['yes, indeed', 'hmm, hmm']"
            .shouldDecodeInto(SimpleStringArray(listOf("yes, indeed", "hmm, hmm")))
        "a = [\"yes, \\\"indeed\\\"\", \"hmm, 'hmm'\"]"
            .shouldDecodeInto(SimpleStringArray(listOf("yes, \"indeed\"", "hmm, 'hmm'")))
        "a = ['yes, \"indeed\"', 'hmm, \"hmm\"']"
            .shouldDecodeInto(SimpleStringArray(listOf("yes, \"indeed\"", "hmm, \"hmm\"")))
    }
}
