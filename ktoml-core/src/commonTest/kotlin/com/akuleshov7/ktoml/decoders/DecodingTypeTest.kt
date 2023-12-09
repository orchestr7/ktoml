package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.exceptions.IllegalTypeException
import kotlinx.serialization.Serializable
import kotlin.test.Test

@Serializable
data class Bool(val a: Boolean)

@Serializable
data class B(val a: Byte)

@Serializable
data class F(val a: Float)

@Serializable
data class D(val a: Double)

@Serializable
data class I(val a: Int)

@Serializable
data class S(val a: Short)

@Serializable
data class L(val a: Long)

@Serializable
data class C(val a: Char)

@Serializable
data class Str(val a: String)

class DecodingTypeTest {
    @Test
    fun testExceptions() {
        "a = true".shouldThrowExceptionWhileDecoding<S, IllegalTypeException>()
        "a = true".shouldThrowExceptionWhileDecoding<B, IllegalTypeException>()
        "a = true".shouldThrowExceptionWhileDecoding<F, IllegalTypeException>()
        "a = true".shouldThrowExceptionWhileDecoding<I, IllegalTypeException>()
        "a = true".shouldThrowExceptionWhileDecoding<C, IllegalTypeException>()

        "a = \"test\"".shouldThrowExceptionWhileDecoding<Bool, IllegalTypeException>()
        "a = true".shouldThrowExceptionWhileDecoding<Str, IllegalTypeException>()
        "a = 12.0".shouldThrowExceptionWhileDecoding<L, IllegalTypeException>()
        "a = 1".shouldThrowExceptionWhileDecoding<D, IllegalTypeException>()
    }
}
