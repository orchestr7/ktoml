package com.akuleshov7.ktoml.test

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.*
import kotlin.test.Test

@ExperimentalSerializationApi
class MainTest {
    class MapDecoder(val map: Map<*, *>, var elementsCount: Int = 0) : AbstractDecoder() {
        private var elementIndex = 0

        override val serializersModule: SerializersModule = EmptySerializersModule

        override fun decodeValue(): Any {
            // of course you should raise a good error here if in your map with values from input you won't find
            // enough values for your serializable class. For example if you class has more fields than there are in the input
            // and these fields are not optional - you should raise an error
            val keyAtTheCurrentIndex = map.keys.elementAt(elementIndex - 1)
            return map[keyAtTheCurrentIndex]!!
        }

        /**
         * this method should be overriden to map between the FIELDS in your class and the VALUE from
         * the input that you would like to inject into it
         */
        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            if (elementIndex == map.size) return CompositeDecoder.DECODE_DONE

            // index of the field from the class where we should inject our value
            val fieldWhereValueShouldBeInjected =
                descriptor.getElementIndex(map.keys.elementAt(elementIndex).toString())

            if (fieldWhereValueShouldBeInjected == CompositeDecoder.UNKNOWN_NAME) {
                // if (weAreStrict) {throw Exception("unknown property") }
            }
            elementIndex++

            return fieldWhereValueShouldBeInjected
        }

        override fun decodeCollectionSize(descriptor: SerialDescriptor): Int =
            decodeInt().also {
                elementsCount = it
            }

        // used to trigger the processing for structures (including nested)
        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
            // corner case at the beginning of the decoding
            if (elementIndex == 0) {
                validateDecodedInput(descriptor, map)
                return MapDecoder(map, descriptor.elementsCount)
            } else {
                // need to decrement element index, as unfortunately it was incremented in the iteration of `decodeElementIndex`
                return when (val innerMap = map.values.elementAt(elementIndex - 1)) {
                    is Map<*, *> -> {
                        validateDecodedInput(descriptor, innerMap)
                        MapDecoder(innerMap, descriptor.elementsCount)
                    }
                    else -> throw Exception("Incorrect nested data")
                }
            }
        }

        override fun decodeNotNullMark(): Boolean = decodeString() != "NULL"

        // we should validate that all keys (required fields) from the class are not missing in the input
        private fun validateDecodedInput(descriptor: SerialDescriptor, map: Map<*, *>) {
            val missingKeysInInput = descriptor.elementNames.toSet() - map.keys.toSet()
            missingKeysInInput.forEach {
                val elementIndex = descriptor.getElementIndex(it.toString())
                if (!descriptor.isElementOptional(elementIndex)) {
                    throw Exception("Invalid number of arguments for deserialization, missing required field" +
                            " ${descriptor.getElementName(elementIndex)}")
                }
            }
        }
    }

    fun <T> decodeFromString(resMap: Map<*, *>, deserializer: DeserializationStrategy<T>): T {
        val decoder = MapDecoder(resMap)
        return decoder.decodeSerializableValue(deserializer)
    }

    inline fun <reified T> decodeFromString(resMap: Map<*, *>): T = decodeFromString(resMap, serializer())

    @Serializable
    data class SerializationClass(
        val a: String = "default value, but provided",
        val b: NestedClass,
        val c: String
    )

    @Serializable
    data class NestedClass(val d: String, val e: String, val f: InnerClass)

    @Serializable
    data class InnerClass(val g: Int, val h: String, val i: Double, val j: String)


    @Test
    fun decodeMapSmallerThanTheClass() {
        // test example of decoding, you can put your parser here, but for simplicity let's avoid it
        // imagine that we would like to serialize the following structure (very similar to json, but very common):
        println("(a:1, b:(d:2, e:3, f:(g:114, h:5, i:6.0, j:7)), c:8)")

        val nestedMap = mapOf("g" to 114, "h" to "5", "i" to 6.0, "j" to "7")
        val initialMap = mapOf("d" to "2", "e" to "3", "f" to nestedMap)
        val resMap = mapOf("a" to "1", "b" to initialMap)

        val obj = decodeFromString<SerializationClass>(resMap)
        println("This is the object that was deserialized: $obj")
    }

    @Test
    fun decodeMapSmallerThanTheClassButAIsDefault() {
        // test example of decoding, you can put your parser here, but for simplicity let's avoid it
        // imagine that we would like to serialize the following structure (very similar to json, but very common):
        println("(a:1, b:(d:2, e:3, f:(g:114, h:5, i:6.0, j:7)), c:8)")

        val nestedMap = mapOf("g" to 114, "h" to "5", "i" to 6.0, "j" to "7")
        val initialMap = mapOf("d" to "2", "e" to "3", "f" to nestedMap)
        val resMap = mapOf("b" to initialMap, "c" to "1")

        val obj = decodeFromString<SerializationClass>(resMap)
        println("This is the object that was deserialized: $obj")
    }

    @Test
    fun decodeMapWithTheSameSizeAsClass() {
        // test example of decoding, you can put your parser here, but for simplicity let's avoid it
        // imagine that we would like to serialize the following structure (very similar to json, but very common):
        println("(a:1, b:(d:2, e:3, f:(g:114, h:5, i:6.0, j:7)), c:8)")

        val nestedMap = mapOf("g" to 114, "h" to "5", "i" to 6.0, "j" to "7")
        val initialMap = mapOf("d" to "2", "e" to "3", "f" to nestedMap)
        val resMap = mapOf("c" to "9", "a" to "1", "b" to initialMap)

        val obj = decodeFromString<SerializationClass>(resMap)
        println("This is the object that was deserialized: $obj")
    }

    @Test
    fun decodeMapSmallerThanTheClassSmall() {
        println("(g:114, h:5, i:6.0, j:7)")
        val nestedMap = mapOf("j" to "7", "h" to "5", "g" to 114)
        val obj = decodeFromString<InnerClass>(nestedMap)
        println("This is the object that was deserialized: $obj")
    }

    // ===========================================
    @Serializable
    data class MyClass(val table1: Table1, val table2: Table2)

    @Serializable
    data class Table1(val a: Int, val b: Int)

    @Serializable
    data class Table2(val a: Int, val inlineTable: InlineTable)

    @Serializable
    data class InlineTable(val a: String)

    @Test
    fun jsonTest() {
        val json = """
        {
          "table1": {
            "a": 5,
            "b": 5
            },
          "table2": {
             "a": 5,
             "inlineTable": {
                 "a": "a"
            }
          }
        }
        """.trimIndent()

        println(Json.decodeFromString<MyClass>(json))
    }


    //====================================

    @OptIn(InternalSerializationApi::class)
    inline fun <reified T : Any> deserializeData(request: String): T {
        return Json.decodeFromString(serializer(), request)
    }

    @Serializable
    data class A<T : Any>(
        val a: T
    )

    @Test
    fun main() {
        println(deserializeData<A<HI>>("{ \"a\": \"1\" }"))
    }
}

typealias HI = Int


