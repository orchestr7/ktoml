package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.Toml
import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlin.test.Test
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

@ExperimentalSerializationApi
class PartialDecoderTest {

    @Serializable
    data class Table1(val a: Long, val b: Long)

    @Test
    fun testPartialDecoding() {
        val parsedResult = Toml.partiallyDecodeFromString<Table1>(
            serializer(),
            "[table1] \n a = 1 \n b = 2 \n [table2] \n c = 1 \n e = 2 \n d = 3",
            "table1"
        )

        parsedResult shouldBe Table1(1, 2)
    }
}
