package com.akuleshov7.ktoml.test.decoder

import com.akuleshov7.ktoml.deserializeToml
import com.akuleshov7.ktoml.deserializeTomlFile
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalSerializationApi
class PartialDecoderTest {
    @Serializable
    data class TwoTomlTables(val table1: Table1, val table2: Table2)

    @Serializable
    data class Table1(val a: Int, val b: Int)

    @Serializable
    data class Table2(val c: Int, val e: Int, val d: Int)

    @Test
    fun testPartialDecoding() {
        val test = TwoTomlTables(Table1(1, 2), Table2(1,2,3))
        assertEquals(test.table1,
            "[table1] \n a = 1 \n b = 2 \n [table2] \n c = 1 \n e = 2 \n d = 3".deserializeToml("table1"))
    }

    @Test
    fun testPartialFileDecoding() {
        val file = "src/commonTest/resources/partial_decoder.toml"
        val test = TwoTomlTables(Table1(1, 2), Table2(1,2,3))
        assertEquals(test.table1,
            file.deserializeTomlFile("table1")
        )
    }
}
