package com.akuleshov7.ktoml.parsers

import com.akuleshov7.ktoml.Toml
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals

@Serializable
data class ProjectOptionalDeps(
    val standard: List<String>
)

@Serializable
data class Project(
    val name: String,
    val version: String,
    val `optional-dependencies`: ProjectOptionalDeps
)

@Serializable
data class PyProject(
    val project: Project
)

/**
 * Regression tests for issue #361: Empty lines after table headers
 * https://github.com/orchestr7/ktoml/issues/361
 *
 * These tests verify that ktoml correctly handles empty lines after TOML table headers,
 * which is valid TOML 1.0 but was previously causing parsing failures.
 */
class Issue361Test {

    @Test
    fun testFastAPIWithEmptyLineAfterTableHeader() {
        // This is the exact TOML structure from issue #361 (FastAPI pyproject.toml)
        // Previously failed with: MissingRequiredPropertyException
        val toml = """
[project]
name = "fastapi"
version = "0.115.6"

[project.optional-dependencies]

standard = [
    "fastapi-cli[standard] >=0.0.8",
    "httpx >=0.23.0"
]
        """.trimIndent()

        val parsed = Toml.decodeFromString<PyProject>(toml)
        assertEquals("fastapi", parsed.project.name)
        assertEquals("0.115.6", parsed.project.version)
        assertEquals(2, parsed.project.`optional-dependencies`.standard.size)
        assertEquals("fastapi-cli[standard] >=0.0.8", parsed.project.`optional-dependencies`.standard[0])
    }

    @Test
    fun testMultipleEmptyLinesAfterTableHeader() {
        // Test with multiple consecutive empty lines
        val toml = """
[project]
name = "test"
version = "1.0.0"

[project.optional-dependencies]



standard = ["dep1", "dep2"]
        """.trimIndent()

        val parsed = Toml.decodeFromString<PyProject>(toml)
        assertEquals("test", parsed.project.name)
        assertEquals(2, parsed.project.`optional-dependencies`.standard.size)
    }

    @Test
    fun testEmptyLinesWithCommentsAfterTableHeader() {
        // Test empty lines mixed with comments
        val toml = """
[project]
name = "test"
version = "1.0.0"

[project.optional-dependencies]

# This is a comment

standard = ["dep1"]
        """.trimIndent()

        val parsed = Toml.decodeFromString<PyProject>(toml)
        assertEquals(1, parsed.project.`optional-dependencies`.standard.size)
    }

    @Test
    fun testNestedTableWithEmptyLineAfterHeader() {
        // Test deeply nested tables with empty lines
        val toml = """
[a.b.c]

value = "test"
        """.trimIndent()

        val parsed = Toml.decodeFromString<DeepNested>(toml)
        assertEquals("test", parsed.a.b.c.value)
    }
}

@Serializable
data class DeepNested(
    val a: ALevel
)

@Serializable
data class ALevel(
    val b: BLevel
)

@Serializable
data class BLevel(
    val c: CLevel
)

@Serializable
data class CLevel(
    val value: String
)
