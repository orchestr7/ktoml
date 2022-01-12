package com.akuleshov7.ktoml.test.decoder

import com.akuleshov7.ktoml.KtomlConf
import com.akuleshov7.ktoml.Toml
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalSerializationApi
class PartialDecoderTest {
    @Serializable
    data class TwoTomlTables(val table1: Table1, val table2: Table2)

    @Serializable
    data class Table1(val a: Long, val b: Long)

    @Serializable
    data class Table2(val c: Long, val e: Long, val d: Long)

    @Test
    fun testPartialDecoding() {
        val test = TwoTomlTables(Table1(1, 2), Table2(1, 2, 3))
        assertEquals(
            test.table1,
            Toml.partiallyDecodeFromString(
                serializer(),
                "[table1] \n a = 1 \n b = 2 \n [table2] \n c = 1 \n e = 2 \n d = 3",
                "table1",
                KtomlConf()
            )
        )
    }
}
