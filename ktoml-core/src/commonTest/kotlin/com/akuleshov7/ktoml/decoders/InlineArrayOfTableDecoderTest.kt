package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.Toml
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals

class InlineArrayOfTableDecoderTest {
    @Serializable
    data class Option(
        val name: String,
        val description: String,
    )
    @Serializable
    data class Options(
        val options: List<Option>,
    )

    @Test
    fun decodeInlineArrayOfTableWithSingleElement() {
        val toml1 = """
            [[options]]
            name = "name1"
            description = "description1"
        """.trimIndent()

        val toml2 = """
            options = [{ name = "name1", description = "description1" }]
        """.trimIndent()

        assertEquals(
            Toml.decodeFromString<Options>(toml1),
            Toml.decodeFromString<Options>(toml2),
        )
    }

    @Test
    fun decodeSimpleInlineArrayOfTable() {
        val toml1 = """
            [[options]]
            name = "name1"
            description = "description1"

            [[options]]
            name = "name2"
            description = "description2"
        """.trimIndent()

        val toml2 = """
            options = [{ name = "name1", description = "description1" }, { name = "name2", description = "description2" }]
        """.trimIndent()

        assertEquals(
            Toml.decodeFromString<Options>(toml1),
            Toml.decodeFromString<Options>(toml2),
        )
    }

    @Test
    fun decodeTomlWithExtraSpaces() {
        var toml1 = """
            [[options]]
            name = "name1"
            description = "description1"
        """.trimIndent()

        var toml2 = """
            options = [     {   name = "name1",     description = "description1"}    ]
        """.trimIndent()

        assertEquals(
            Toml.decodeFromString<Options>(toml1),
            Toml.decodeFromString<Options>(toml2),
        )

        toml1 = """
            [[options]]
            name = "name1"
            description = "description1"

            [[options]]
            name = "name2"
            description = "description2"
        """.trimIndent()

        toml2 = """
            options = [  { name = "name1", description = "description1" },   {   name = "name2", description = "description2" }  ]
        """.trimIndent()

        assertEquals(
            Toml.decodeFromString<Options>(toml1),
            Toml.decodeFromString<Options>(toml2),
        )
    }

    @Serializable
    data class Point(
        val x: Int,
        val y: Int,
        val z: Int,
    )
    @Serializable
    data class Points(
        val points: List<Point>,
    )

    @Test
    fun decodeMultilineInlineArrayOfTable() {
        val toml = """
            points = [  { x = 1, y = 2, z = 3 },
                        { x = 7, y = 8, z = 9 },
                        { x = 2, y = 4, z = 8 }
                     ]
        """.trimIndent()

        assertEquals(
            Points(
                points = listOf(
                    Point(1, 2, 3),
                    Point(7, 8, 9),
                    Point(2, 4, 8),
                ),
            ),
            Toml.decodeFromString<Points>(toml),
        )
    }

    @Test
    fun decodeMultilineWithTrailingComma() {
        val toml = """
            points = [  { x = 1, y = 2, z = 3 },
                        { x = 7, y = 8, z = 9 } ,
                        { x = 2, y = 4, z = 8 },
                     ]
        """.trimIndent()

        assertEquals(
            Points(
                points = listOf(
                    Point(1, 2, 3),
                    Point(7, 8, 9),
                    Point(2, 4, 8),
                ),
            ),
            Toml.decodeFromString<Points>(toml),
        )
    }

    @Test
    fun decodeWithArrayElements() {
        @Serializable
        data class Point(
            val points: List<Int>,
        )
        @Serializable
        data class Value(
            val value: List<Point>,
        )

        val toml = """
            value = [  { points = [1, 2] },
                       { points = [3, 4] }]
        """.trimIndent()
        assertEquals(
            Value(
                value = listOf(
                    Point(points = listOf(1, 2)),
                    Point(points = listOf(3, 4)),
                ),
            ),
            Toml.decodeFromString<Value>(toml),
        )
    }

    @Test
    fun decodeInsideOtherTable() {
        @Serializable
        data class Option(
            val name: String,
            val description: String,
        )
        @Serializable
        data class Command(
            val name: String,
            val description: String,
            val options: List<Option>? = null,
        )
        @Serializable
        data class Commands(
            val command: Command,
        )

        val toml = """
            [command]
	            name = "ping"
	            description = "description"
	            options = [{ name = "user", description = "descr" }]
        """.trimIndent()
        assertEquals(
            Commands(
                command = Command(
                    name = "ping",
                    description = "description",
                    options = listOf(Option(name = "user", description = "descr"))
                )
            ),
            Toml.decodeFromString<Commands>(toml),
        )
    }
}
