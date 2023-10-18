package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlInputConfig
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.test.Test

@Serializable
data class Key(val value: Long)

@Serializable
data class Config1(val key: Key? = null)

@Serializable
data class Config2(val key: Key? = null)

@Serializable
data class Config3(val key: Key = Key(0L))

@Serializable
data class Config(val key: Key? = Key(0L))

@Serializable
data class Config4(val key: Key)

class NullableTablesTest {
    @Test
    fun nullableKey() {
        val mapper = Toml(
            inputConfig = TomlInputConfig(
                ignoreUnknownNames = true,
                allowEmptyValues = true
            )
        )
        val toml1 = mapper.decodeFromString<Config1>(
            """            
            [key]
            value = 1            
        """.trimIndent()
        )

        toml1.shouldNotBeNull()
        toml1.key?.value shouldBe 1L

        val toml2 = mapper.decodeFromString<Config2>(
            """            
            [key]
            value = 1            
        """.trimIndent()
        )

        toml2.shouldNotBeNull()
        toml2.key?.value shouldBe 1L

        val toml3 = mapper.decodeFromString<Config3>(
            """            
            [key]
            value = 1            
        """.trimIndent()
        )

        toml3.shouldNotBeNull()
        toml3.key.value shouldBe 1L

        val toml4 = mapper.decodeFromString<Config4>(
            """            
            [key]
            value = 1            
        """.trimIndent()
        )

        toml4.shouldNotBeNull()
        toml4.key.value shouldBe 1L
    }
}

class EmptyTomlTest {
    @Test
    fun emptyToml() {
        var res = Toml.decodeFromString<Config>(
            """            
             
             
                """.trimIndent()
        )

        res shouldBe Config()

        res = Toml.decodeFromString(
            "".trimIndent()
        )

        res shouldBe Config()
    }
}
