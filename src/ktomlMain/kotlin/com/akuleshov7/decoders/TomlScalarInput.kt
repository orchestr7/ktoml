package com.akuleshov7.decoders

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.modules.SerializersModule

@OptIn(ExperimentalSerializationApi::class)
public class TomlScalarInput(content: String, serializersModule: SerializersModule,) : TomlInput(content, serializersModule) {
    override fun decodeString(): String = content
    override fun decodeInt(): Int = content.toInt()
    override fun decodeLong(): Long = content.toLong()
    override fun decodeShort(): Short = content.toShort()
    override fun decodeByte(): Byte = content.toByte()
    override fun decodeDouble(): Double = content.toDouble()
    override fun decodeFloat(): Float = content.toFloat()
    override fun decodeBoolean(): Boolean = content.toBoolean()
    override fun decodeChar(): Char = content.singleOrNull()!!

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        TODO()
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int = 0
}
