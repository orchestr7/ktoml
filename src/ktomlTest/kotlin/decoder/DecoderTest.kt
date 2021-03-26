package com.akuleshov7.ktoml.test.decoder

import com.akuleshov7.ktoml.Ktoml
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import kotlin.test.Test

class DecoderTest {
    @OptIn(InternalSerializationApi::class)
    inline fun <reified T : Any> deserialize(request: String): T {
        return Ktoml().decodeFromString(serializer(), request)
    }

    @Serializable
    data class SimpleTomlCase(val table1: Table1)

    @Serializable
    data class Table1(val a: Int, val b: Int)

    @Serializable
    data class Table2(val b: Int, val a: Int)

    @Serializable
    data class TwoTomlTables(val table1: Table1, val table2: Table2)

    @Test
    fun TestForSimpleTomlCase() {
        println("table1: (a:5, b:6)")
        val test = deserialize<SimpleTomlCase>("[table1]\n b = 6  \n a = 5 ")
        println(test)
    }

    @Test
    fun TestForTwoTomlTablesCase() {
        println("table1: (a:5, b:6)")
        val test = deserialize<TwoTomlTables>("[table1]\n b = 6  \n a = 5 \n [table2] \n a = 7 \n b =8")
        println(test)
    }
}