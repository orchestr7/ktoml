package com.akuleshov7.certification

import com.akuleshov7.certification.schemas.*
import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.file.getOsSpecificFileSystem
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okio.Path.Companion.toPath
import kotlin.test.Test

class CertificationTest {
    @Test
    fun `VALID parse TOML files and compare it with JSON result`() {
        val jsonPath = "src/commonTest/resources/toml_certification/valid/array/array.json".toPath()
        val tomlPath = "src/commonTest/resources/toml_certification/valid/array/array.toml".toPath()

        val toml = getOsSpecificFileSystem().read(tomlPath) {
            generateSequence { readUtf8Line() }.toList()
        }

        val json = getOsSpecificFileSystem().read(jsonPath) {
            generateSequence { readUtf8Line() }.toList()
        }

        println(
            Json.decodeFromString<ArrayCertification>(json.joinToString("\n"))
                == Toml.decodeFromString<ArrayCertification>(toml.joinToString("\n"))
        )
    }

    @Test
    fun `INVALID fail during parsing of TOML`() {

    }
}
