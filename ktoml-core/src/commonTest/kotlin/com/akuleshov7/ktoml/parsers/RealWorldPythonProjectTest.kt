package com.akuleshov7.ktoml.parsers

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlInputConfig
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

/**
 * Real-world test cases from popular Python projects.
 * These tests validate ktoml against actual pyproject.toml files used in production.
 */
class RealWorldPythonProjectTest {

    @Serializable
    data class TestProject(
        val project: ProjectSection
    )

    @Serializable
    data class ProjectSection(
        val name: String,
        val version: String
    )

    @Serializable
    data class SimpleTable(
        val table: TableContent
    )

    @Serializable
    data class TableContent(
        val key: String
    )

    /**
     * Test parsing FastAPI's pyproject.toml structure.
     *
     * FastAPI has an empty line after [project.optional-dependencies] which is valid TOML
     * but triggers a ktoml parser bug:
     * https://github.com/orchestr7/ktoml/issues/361
     */
    @Test
    fun testFastAPIStyleEmptyLineAfterTableHeader() {
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

        // This should parse successfully - empty lines after table headers are valid TOML
        // Test that the parser can handle this structure
        val parser = TomlParser(TomlInputConfig())
        val tree = parser.parseString(toml)
        assertNotNull(tree)
        assertTrue(tree.children.isNotEmpty())
    }

    /**
     * Simplified reproduction of the empty line issue.
     */
    @Test
    fun testEmptyLineAfterTableHeader() {
        val toml = """
            [table]

            key = "value"
        """.trimIndent()

        // Empty lines after table headers should be allowed
        val result = Toml.decodeFromString<SimpleTable>(toml)
        assertNotNull(result)
    }

    /**
     * Test multiple empty lines after table header.
     */
    @Test
    fun testMultipleEmptyLinesAfterTableHeader() {
        val toml = """
            [table]


            key = "value"
        """.trimIndent()

        val result = Toml.decodeFromString<SimpleTable>(toml)
        assertNotNull(result)
    }
}
