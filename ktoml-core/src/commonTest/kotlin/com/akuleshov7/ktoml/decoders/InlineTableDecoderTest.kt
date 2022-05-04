package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.exceptions.ParseException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.test.*

class InlineTableDecoderTest {
    @Serializable
    data class GradleExample(val plugins: ListOfInlines)

    @Serializable
    data class ListOfInlines(
        @SerialName("kotlin-jvm")
        val kotlinJvm: Plugin,

        @SerialName("kotlin-multiplatform")
        val kotlinMultiplatform: Plugin,

        @SerialName("kotlin-plugin-serialization")
        val kotlinPLuginSerialization: Plugin,
    )

    @Serializable
    data class Plugin(val id: String, val version: Version)

    @Serializable
    data class Version(val ref: String)

    @Test
    @Ignore
    fun decodeInlineTable() {
        val test =
            """
            |someBooleanProperty = true
            |
            |table1 = { property1 = null, property2 = 6 }
            |table2 = { someNumber = 5, table2."akuleshov7.com" = { name = 'this is a "literal" string', configurationList = ["a",  "b",  "c", null] }}
            |table2 = { otherNumber = 5.56 }
            |inlineTable = { inlineValStr = "inline", inlineValInt = -1 }
            |       
            """.trimMargin()

        Toml.decodeFromString<ReadMeExampleTest.MyClass>(test)
    }

    @Test
    fun trailingCommaIsNotPermitted() {
        val test =
            """
            |inlineTable = { inlineValStr = "inline", inlineValInt = -1, }
            |       
            """.trimMargin()

        assertFailsWith<ParseException> { Toml.decodeFromString<ReadMeExampleTest.MyClass>(test) }
    }

    @Test
    fun gradleLibsToml() {
        val test =
            """
                |[plugins]
                |kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
                |kotlin-multiplatform = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
                |kotlin-plugin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
            """.trimMargin()

        val decoded = Toml.decodeFromString<GradleExample>(test)

        assertEquals(
            GradleExample(ListOfInlines(
                Plugin("org.jetbrains.kotlin.jvm", Version("kotlin")),
                Plugin("org.jetbrains.kotlin.jvm", Version("kotlin")),
                Plugin("org.jetbrains.kotlin.plugin.serialization", Version("kotlin")))
            ),
            decoded
        )
    }
}