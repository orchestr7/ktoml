package com.akuleshov7.ktoml.encoders

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.test.Test

class ValueClassEncoderTest {

    @Serializable
    @JvmInline
    value class Color(val rgb: Int)

    @Serializable
    data class NamedColor(val color: Color, val name: String)

    @Test
    fun testForSimpleValueClass() {
        Color(15).shouldEncodeInto("rgb = 15")
    }

    @Test
    fun testForNestedValueClass() {
        NamedColor(Color(150), "black")
            .shouldEncodeInto(
                """
                color = 150
                name = "black"
                """.trimIndent()
            )
    }

    @Test
    fun testForLisOfValueClass() {
        @Serializable
        class Palette(val colors: List<Color>)

        Palette(
            listOf(
                Color(0),
                Color(255),
                Color(128),
            )
        ).shouldEncodeInto("colors = [ 0, 255, 128 ]")
    }

    @Serializable
    @JvmInline
    value class Num(val int: Int)

    @Serializable
    data class Nums(
        val num1: Num,
        val num2: Num,
    )

    @Test
    fun testForMultipleValueClass() {
        Nums(
            num1 = Num(5),
            num2 = Num(111)
        ).shouldEncodeInto(
            """
            num1 = 5
            num2 = 111
            """.trimIndent()
        )
    }

    @Serializable
    data class MyObject(
        val height: Int,
        val width: Int
    )

    @Serializable
    @JvmInline
    value class Info(val obj: MyObject)

    @Serializable
    data class InfoWrapper(
        val metaInfo1: Int,
        val info: Info,
        val metaInfo2: String
    )

    @Test
    fun testForValueClassWithObjectInside() {
        InfoWrapper(
            metaInfo1 = 1,
            info = Info(MyObject(10, 20)),
            metaInfo2 = "test"
        ).shouldEncodeInto(
            """
            metaInfo1 = 1
            metaInfo2 = "test"
            
            [info]
                height = 10
                width = 20
            """.trimIndent()
        )
    }

    @Serializable
    @JvmInline
    value class AnotherInfoWrapper(val info: Info)

    @Test
    fun testForValueClassInsideValueClass() {
        AnotherInfoWrapper(Info(MyObject(10, 20)))
            .shouldEncodeInto(
                """
                [info]
                    height = 10
                    width = 20
                """.trimIndent()
            )
    }

    @Test
    fun testDataClassInsideValueClass() {
        Info(MyObject(32, 64))
            .shouldEncodeInto(
                """
                [obj]
                    height = 32
                    width = 64
                """.trimIndent()
            )
    }
}
