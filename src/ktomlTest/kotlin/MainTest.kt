package com.akuleshov7.ktoml.test

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.*
import kotlin.test.Test



@ExperimentalSerializationApi
class MainTest {
    class ListDecoder(val list: String, var elementsCount: Int = 0) : AbstractDecoder() {
        private var elementIndex = 0

        override val serializersModule: SerializersModule = EmptySerializersModule

        override fun decodeValue(): Any {
            return list.split(",")[elementIndex - 1]
        }

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            if (elementIndex == elementsCount) return CompositeDecoder.DECODE_DONE
            println(elementIndex)
            return elementIndex++
        }

        override fun decodeCollectionSize(descriptor: SerialDescriptor): Int =
            decodeInt().also { elementsCount = it }

        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
            ListDecoder(list, descriptor.elementsCount)

        // sequential decoding not applicable in this case as elements are not ordered
        override fun decodeSequentially(): Boolean = false

        override fun decodeNotNullMark(): Boolean = decodeString() != "NULL"
    }

    fun <T> decodeFromString(str: String, deserializer: DeserializationStrategy<T>): T {
        val decoder = ListDecoder(str)
        return decoder.decodeSerializableValue(deserializer)
    }

    inline fun <reified T> decodeFromString(str: String): T = decodeFromString(str, serializer())

    @Serializable
    data class Project(val name: String, val owner: String, val votes: String)

    @Serializable
    data class User(val name: String)

    @Test
    fun main() {
        val str = listOf<String>("[A]", "a = 5")
       // val obj = decodeFromString<Project>(str)
       // println(obj)
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
}
