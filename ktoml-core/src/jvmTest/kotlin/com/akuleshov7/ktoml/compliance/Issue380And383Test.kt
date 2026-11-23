package com.akuleshov7.ktoml.compliance

import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.exceptions.TomlDecodingException
import com.akuleshov7.ktoml.parsers.TomlParser
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class Issue380And383Test {
    private val parser = TomlParser(TomlInputConfig.compliant())
    private val testDir = File("../toml-test/tests")

    @Test
    fun parsesSpecArray0() {
        val testFile = testDir.resolve("valid/spec-1.0.0/array-0.toml")
        val expectedJsonFile = testDir.resolve("valid/spec-1.0.0/array-0.json")

        val actualJson = TomlTestConverter.toJson(parser.parseString(testFile.readText()))
        val expectedJson = Json.parseToJsonElement(expectedJsonFile.readText())

        assertEquals(expectedJson, actualJson)
    }

    @Test
    fun rejectsInvalidInlineTableRedefinitions() {
        listOf(
            "invalid/inline-table/duplicate-key-01.toml",
            "invalid/inline-table/duplicate-key-02.toml",
            "invalid/inline-table/duplicate-key-03.toml",
            "invalid/inline-table/overwrite-06.toml",
            "invalid/inline-table/overwrite-08.toml",
            "invalid/inline-table/overwrite-10.toml",
            "invalid/spec-1.0.0/inline-table-2-0.toml",
            "invalid/spec-1.0.0/inline-table-3-0.toml",
            "invalid/spec-1.0.0/table-9-0.toml",
            "invalid/spec-1.0.0/table-9-1.toml",
            "invalid/table/append-with-dotted-keys-07.toml",
        ).forEach { testPath ->
            val exception = assertFailsWith<TomlDecodingException>("Expected $testPath to be rejected") {
                parser.parseString(testDir.resolve(testPath).readText())
            }
            assertTrue(exception.message.orEmpty().startsWith("Line "))
        }
    }

    @Test
    fun rejectsArrayWithOnlyComma() {
        val testPath = "invalid/array/only-comma-01.toml"
        val exception = assertFailsWith<TomlDecodingException>("Expected $testPath to be rejected") {
            parser.parseString(testDir.resolve(testPath).readText())
        }
        assertTrue(exception.message.orEmpty().startsWith("Line 1:"))
    }
}
