package com.akuleshov7.ktoml.decoders

import kotlinx.serialization.Serializable
import kotlin.test.Ignore
import kotlin.test.Test

@Serializable
data class TomlArrayOfTables(val a: List<Long>)

class ArrayOfTablesDecoderTest {
    @Test
    @Ignore
    fun decodeArrayOfTables() {
    }
}
