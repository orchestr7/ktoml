package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.exceptions.CastException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@Serializable
data class TomlArrayOfTables(val a: List<Long>)

class ArrayOfTablesDecoderTest {
    @Test
    fun testRegressions() {


    }
}
