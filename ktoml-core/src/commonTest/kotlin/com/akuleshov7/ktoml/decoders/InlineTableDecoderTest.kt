package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.exceptions.ParseException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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
        """
            |someBooleanProperty = true
            |
            |table1 = { property1 = null, property2 = 6 }
            |table2 = { someNumber = 5, table2."akuleshov7.com" = { name = 'this is a "literal" string', configurationList = ["a",  "b",  "c", null] }}
            |table2 = { otherNumber = 5.56 }
            |inlineTable = { inlineValStr = "inline", inlineValInt = -1 }
            |       
        """.trimMargin()
//            .shouldDecodeInto(ReadMeExampleTest.MyClass(...))
    }

    @Test
    fun trailingCommaIsNotPermitted() {
        """
            |inlineTable = { inlineValStr = "inline", inlineValInt = -1, }
            |       
        """.trimMargin()
            .shouldThrowExceptionWhileDecoding<ReadMeExampleTest.MyClass, ParseException>()
    }

    @Test
    fun gradleLibsToml() {
        """
            |[plugins]
            |kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
            |kotlin-multiplatform = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
            |kotlin-plugin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
        """.trimMargin()
            .shouldDecodeInto(
                GradleExample(
                    ListOfInlines(
                        Plugin("org.jetbrains.kotlin.jvm", Version("kotlin")),
                        Plugin("org.jetbrains.kotlin.jvm", Version("kotlin")),
                        Plugin("org.jetbrains.kotlin.plugin.serialization", Version("kotlin"))
                    )
                )
            )
    }

    @Serializable
    data class Point(val x: Int? = null, val y: Int? = null)

    @Serializable
    data class Position(val point: Point)

    @Serializable
    data class PositionWrapper(
        val id: Int,
        val position: Position,
        val description: String
    )

    @Test
    fun testEmptyInlineTable() {
        """
            point = {  }
        """.trimIndent()
            .shouldDecodeInto(Position(point = Point()))

        """
            [point] 
        """.trimIndent()
            .shouldDecodeInto(Position(point = Point()))
    }

    @Test
    fun testNestedEmptyInlineTable() {
        """
            id = 15
            description = "abc"

            [position]
                point = {}
        """.trimIndent()
            .shouldDecodeInto(
                PositionWrapper(
                    id = 15,
                    description = "abc",
                    position = Position(
                        point = Point()
                    )
                )
            )
    }
}