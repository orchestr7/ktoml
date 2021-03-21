package com.akuleshov7.ktoml.test

import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlin.test.Test

@ExperimentalSerializationApi
class JsonTomlComparissonTest {
    // This test will compare the behavior of ktoml serializer and json serializer
    /*
     * [table1]
     *    a = 5
     *    b = 6
     *
     * [table2]
     *    a = 5
     *
     *    [table2.inlineTable]
     *         a = "a"
     */

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
