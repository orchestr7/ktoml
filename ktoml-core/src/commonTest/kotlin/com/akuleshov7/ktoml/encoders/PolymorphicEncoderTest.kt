package com.akuleshov7.ktoml.encoders

import com.akuleshov7.ktoml.annotations.TomlInlineTable
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.serialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.test.Test

@OptIn(ExperimentalSerializationApi::class)
class PolymorphicEncoderTest {
    @Serializable
    sealed class Parent {
        @Serializable(ChildA.InPlaceSerializer::class)
        @SerialName("childA")
        class ChildA(val list: List<Long> = listOf(1, 2, 3)) : Parent() {
            object InPlaceSerializer : KSerializer<ChildA> {
                override val descriptor = SerialDescriptor("childA", serialDescriptor<List<Long>>())

                override fun serialize(encoder: Encoder, value: ChildA) {
                    serializer<List<Long>>().serialize(encoder, value.list)
                }

                override fun deserialize(decoder: Decoder): Nothing = throw IllegalStateException()
            }
        }

        @Serializable(ChildB.InPlaceSerializer::class)
        @SerialName("childB")
        class ChildB(val value: String = "string") : Parent() {
            object InPlaceSerializer : KSerializer<ChildB> {
                override val descriptor = PrimitiveSerialDescriptor("childB", PrimitiveKind.STRING)

                override fun serialize(encoder: Encoder, value: ChildB) {
                    encoder.encodeString(value.value)
                }

                override fun deserialize(decoder: Decoder): Nothing = throw IllegalStateException()
            }
        }

        @Serializable(ChildC.InPlaceSerializer::class)
        @SerialName("childC")
        class ChildC(val value: Enum = Enum.A) : Parent() {
            @Serializable
            enum class Enum { A }

            object InPlaceSerializer : KSerializer<ChildC> {
                override val descriptor = SerialDescriptor("childC", serialDescriptor<Enum>())

                override fun serialize(encoder: Encoder, value: ChildC) {
                    encoder.encodeEnum(serialDescriptor<Enum>(), value.value.ordinal)
                }

                override fun deserialize(decoder: Decoder): Nothing = throw IllegalStateException()
            }
        }
    }

    @Test
    fun sealedTableArrayTest() {
        @Serializable
        class File(
            val polymorphicTables: List<Parent> = listOf(
                Parent.ChildA(),
                Parent.ChildB(),
                Parent.ChildC()
            )
        )

        File().shouldEncodeInto(
            """
                [[polymorphicTables]]
                    type = "childA"
                    value = [ 1, 2, 3 ]
                
                [[polymorphicTables]]
                    type = "childB"
                    value = "string"
                
                [[polymorphicTables]]
                    type = "childC"
                    value = "A"
            """.trimIndent()
        )
    }

    @Test
    fun sealedArrayElementTest() {
        @Serializable
        class File(
            @TomlInlineTable
            val polymorphicArray: List<Parent> = listOf(
                Parent.ChildA(),
                Parent.ChildB(),
                Parent.ChildC()
            )
        )

        File().shouldEncodeInto(
            """polymorphicArray = [ [ "childA", [ 1, 2, 3 ] ], [ "childB", "string" ], [ "childC", "A" ] ]"""
        )
    }
}
