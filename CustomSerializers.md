### Customizing ktoml serialization
This page will be useful ONLY for those who plan to customize the serialization and deserialization logic of ktoml.

### Custom Deserializer
We suggest to use custom deserializer only for **very primitive cases**. 
In case you **really** need proper custom serializer, you will need to have something like this (this is a real generated code for the serialization):

```kotlin
override fun deserialize(decoder: Decoder): Color {
    val serialDescriptor = descriptor
    var bl = true
    var n = 0
    var l = 0L
    // to read TOML AST properly - you need to run beginStructure first
    val compositeDecoder = decoder.beginStructure(serialDescriptor)
    block4@ while (bl) {
        val n2 = compositeDecoder.decodeElementIndex(serialDescriptor)
        when (n2) {
            // decoding done:
            -1 -> {
                bl = false
                continue@block4
            }
            // element index
            0 -> {
                l = compositeDecoder.decodeLongElement(serialDescriptor, 0)
                n = n or 1
                continue@block4
            }
        }
        throw IllegalArgumentException()
    }
    compositeDecoder.endStructure(serialDescriptor)
    return Color(l)
}
```

for the following data class:

```kotlin
    @Serializable(with = ColorAsStringSerializer::class)
    data class Color(val rgb: Long)
```

### Simple cases for Deserialization
Imaging you have your class Color:
```kotlin
    @Serializable(with = ColorAsStringSerializer::class)
    data class Color(val rgb: Long)
```

We have several small workarounds, that would let you override deserializers for your class and using ktoml-native parser and AST:

```kotlin
    object ColorAsStringSerializer : KSerializer<Color> {
        override fun deserialize(decoder: Decoder): Color {
            // please note, that the decoder should match with the real type of a variable:
            // for string in the input - decodeString, for long - decodeLong, etc.
            val string = decoder.decodeString()
            return Color(string.toLong() + 15)
        }
    }
```

```kotlin
    Toml.decodeFromString<Color>(
        """
            rgb = "0"
        """.trimIndent()
    )
```
