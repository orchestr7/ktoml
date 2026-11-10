package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.Toml
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The decoder counterpart to `PolymorphicEncoderTest`: a heterogeneous collection whose elements have
 * different concrete types is the "typed" answer to mixed-type content (orchestr7/ktoml#317). When the
 * variant is selected by a discriminator carried *in the document*, a plain `@Serializable sealed class`
 * decodes with no custom serializer at all — kotlinx.serialization's default polymorphism reads the
 * `type` discriminator and dispatches.
 *
 * This works precisely because the discriminator is ordinary decoded data; the decoder never has to
 * peek the underlying TOML type of an untagged value (which is the one thing a consumer-side serializer
 * cannot do — see [MixedTypeArrayDecodeTest]).
 */
class PolymorphicDecoderTest {
    @Serializable
    private sealed class Shape {
        @Serializable
        @SerialName("circle")
        data class Circle(val radius: Int) : Shape()

        @Serializable
        @SerialName("rect")
        data class Rect(val w: Int, val h: Int) : Shape()
    }

    @Serializable
    private data class Doc(val shapes: List<Shape>)

    @Test
    fun sealedClassRoundTripsThroughDefaultPolymorphism() {
        val doc = Doc(listOf(Shape.Circle(5), Shape.Rect(3, 4)))

        // encode → decode must reproduce the original heterogeneous list
        assertEquals(doc, Toml.decodeFromString<Doc>(Toml.encodeToString(doc)))
    }

    @Test
    fun decodeSealedClassFromLiteral() {
        // kotlinx default polymorphism represents a variant as `type` + a nested `value` table
        //language=toml
        val input = """
            [[shapes]]
            type = "circle"
            [shapes.value]
            radius = 5

            [[shapes]]
            type = "rect"
            [shapes.value]
            w = 3
            h = 4
        """.trimIndent()

        assertEquals(
            Doc(listOf(Shape.Circle(5), Shape.Rect(3, 4))),
            Toml.decodeFromString<Doc>(input),
        )
    }
}
