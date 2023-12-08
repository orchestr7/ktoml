package com.akuleshov7.ktoml.encoders

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlOutputConfig
import io.kotest.matchers.should
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.test.Test

class ClassEncoderTest {
    @Serializable
    object Object

    @Test
    fun objectTest() {
        @Serializable
        data class File(val obj: Object = Object)

        File() should encodeInto("[obj]")
    }

    @Test
    fun keyQuotingTest() {
        @Serializable
        data class Table(val value: String)

        @Serializable
        data class ParentTable(
            @SerialName("tableⱯ")
            val basicQuotedTable: Table = Table("a"),
            @SerialName("\"tableꓭ\"")
            val literalQuotedTable: Table = Table("b")
        )

        @Serializable
        data class File(
            @SerialName("key with spaces")
            val basicQuotedProperty: Boolean = false,
            @SerialName("keyWith\"Quotes\"")
            val literalQuotedProperty: Long = 3,
            val parent: ParentTable = ParentTable()
        )

        File() should encodeInto(
            """
                "key with spaces" = false
                'keyWith"Quotes"' = 3
                
                [parent."tableⱯ"]
                    value = "a"
                
                [parent.'"tableꓭ"']
                    value = "b"
            """.trimIndent()
        )
    }

    @Test
    fun pairReorderingTest() {
        @Serializable
        data class Table(
            val c: String = "value",
            val tableB: Object = Object,
            val d: Boolean = false
        )

        @Serializable
        data class File(
            val a: Long = 0,
            val tableA: Table = Table(),
            val b: List<Long> = listOf(1, 2, 3)
        )

        File() should encodeInto(
            """
                a = 0
                b = [ 1, 2, 3 ]
                
                [tableA]
                    c = "value"
                    d = false
                
                    [tableA.tableB]
            """.trimIndent()
        )
    }

    @Test
    fun defaultOmissionTest() {
        @Serializable
        data class Table(
            val omitted: String = "",
            val present: String
        )

        @Serializable
        data class File(
            val omitted: Boolean = false,
            val present: Boolean,
            val omittedTable: Table = Table(present = "value"),
            val presentTable: Table,
        )

        File(
            present = true,
            presentTable = Table(present = "value")
        ) should encodeInto(
            """
                present = true
                
                [presentTable]
                    present = "value"
            """.trimIndent(),
            tomlInstance = Toml(
                outputConfig = TomlOutputConfig(
                    ignoreDefaultValues = true
                )
            )
        )
    }

    @Test
    fun nullOmissionTest() {
        @Serializable
        data class Table(
            val omitted: Long? = null,
            val present: Long? = 5
        )

        @Serializable
        data class File(
            val omitted: Double? = null,
            val present: Double = Double.NaN,
            val omittedTable: Table? = null,
            val presentTable: Table? = Table()
        )

        File() should encodeInto(
            """
               present = nan
               
               [presentTable]
                   present = 5
            """.trimIndent()
        )
    }
}
