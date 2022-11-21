package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.exceptions.ParseException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.Serializable

class PrimitivesDecoderTest {

  @Test
  fun decodeByte() {
    fun test(expected: Byte, actual: String) {
      val toml = /*language=TOML*/ """value = $actual"""

      @Serializable
      data class Data(val value: Byte)

      val data = Toml.decodeFromString(Data.serializer(), toml)

      assertEquals(expected, data.value)
    }

    test(0, "0")
    test(1, "1")
    test(-1, "-1")
    test(-128, "-128")
    test(127, "127")
  }

  @Test
  fun decodeByteFailure() {
    fun testFails(input: String) {
      val toml = /*language=TOML*/ """value = $input"""

      @Serializable
      data class Data(val value: Byte)

      assertFailsWith<ParseException>(input) {
        Toml.decodeFromString(Data.serializer(), toml)
      }
    }

    testFails("-129")
    testFails("128")
  }

  @Test
  fun decodeChar() {
    fun test(expected: Char, actual: String) {
      val toml = /*language=TOML*/ """value = $actual"""

      @Serializable
      data class Data(val value: Char)

      val data = Toml.decodeFromString(Data.serializer(), toml)

      assertEquals(expected, data.value)
    }

    test((0).toChar(), "0")
    test((-1).toChar(), "-1")
    test((1).toChar(), "1")
    test(Char.MAX_VALUE, "0")
    test(Char.MIN_VALUE, "1")
  }

  @Test
  fun decodeCharFailure() {
    fun testFails(input: String) {
      val toml = /*language=TOML*/ """value = $input"""

      @Serializable
      data class Data(val value: Byte)

      assertFailsWith<ParseException>(input) {
        Toml.decodeFromString(Data.serializer(), toml)
      }
    }

    testFails("${Char.MAX_VALUE.digitToInt() + 1}")
    testFails("${Char.MIN_VALUE.digitToInt() - 1}")
  }

  @Test
  fun decodeShort() {
    val toml = /*language=TOML*/ """value = 1"""

    @Serializable
    data class Data(val value: Short)

    val data = Toml.decodeFromString(Data.serializer(), toml)

    assertEquals(1, data.value)
  }

  @Test
  fun decodeInt() {
    val toml = /*language=TOML*/ """value = 1"""

    @Serializable
    data class Data(val value: Int)

    val data = Toml.decodeFromString(Data.serializer(), toml)

    assertEquals(1, data.value)
  }

  @Test
  fun decodeLong() {
    val toml = /*language=TOML*/ """value = 1"""

    @Serializable
    data class Data(val value: Long)

    val data = Toml.decodeFromString(Data.serializer(), toml)

    assertEquals(1, data.value)
  }

  @Test
  fun decodeFloat() {
    val toml = /*language=TOML*/ """value = 1"""

    @Serializable
    data class Data(val value: Double)

    val data = Toml.decodeFromString(Data.serializer(), toml)

    assertEquals(1.0, data.value)
  }

  @Test
  fun decodeDouble() {
    val toml = /*language=TOML*/ """value = 1"""

    @Serializable
    data class Data(val value: Double)

    val data = Toml.decodeFromString(Data.serializer(), toml)

    assertEquals(1.0, data.value)
  }

}
