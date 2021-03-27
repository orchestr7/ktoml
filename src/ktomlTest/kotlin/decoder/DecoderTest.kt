package com.akuleshov7.ktoml.test.decoder

import com.akuleshov7.ktoml.Ktoml
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertEquals

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
    data class Table2(val c: Int, val e: Int, val d: Int)

    @Serializable
    data class TwoTomlTables(val table1: Table1, val table2: Table2)

    @Test
    fun TestForSimpleTomlCase() {
        println("table1: (a:5, b:6)")
        val test = deserialize<SimpleTomlCase>("[table1]\n b = 6  \n a = 5 ")
        assertEquals(SimpleTomlCase(Table1(5, 6)), test)
    }

    @Test
    fun TestForTwoTomlTablesCase() {
        println("table1: (b:6, a:5), table2:(c:7, d:8)")
        val test = deserialize<TwoTomlTables>(
            "[table1]\n" +
                    " b = 6  \n" +
                    " a = 5  \n " +

            "[table2] \n" +
                    " c = 7  \n" +
                    " d = 8  \n" +
                    " e = 9 \n"


        )
        assertEquals(TwoTomlTables(Table1(5, 6), Table2(7, 9, 8)), test)
    }
}