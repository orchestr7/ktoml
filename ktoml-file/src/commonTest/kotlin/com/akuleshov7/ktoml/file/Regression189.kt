package com.akuleshov7.ktoml.file

import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import kotlin.test.Test

class Regression189 {
    @Serializable
    data class ServerSettings(
        val email: String,
        val url: String,
        val deviceId: String,
        val bearerToken: String?,
        val refreshToken: String?,
    )

    @ExperimentalSerializationApi
    @Test
    fun regressionCastTest() {
        val file = "src/commonTest/resources/regression_189.toml"
        val parsedResult = TomlFileReader.decodeFromFile<ServerSettings>(serializer(), file)
        parsedResult shouldBe ServerSettings(
            "test5",
            "http://localhost:8080",
            "50694ed7-a93f-4713-9e55-4d512ce2e4db",
            "a8DkRGThvz13cmVubFdgX0CsoLfAtXcBvyxiKCPY34FEt3UDmPBkMKFRk4iKRuRp",
            "20NSBgKB2B9C2u2toAuiPqlaZgfEWJ4m50562YK9w575SNt31CWrjcpwqeiDCYhZ"
        )
    }
}