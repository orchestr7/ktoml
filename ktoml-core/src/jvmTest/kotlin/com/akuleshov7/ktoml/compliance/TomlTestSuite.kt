package com.akuleshov7.ktoml.compliance

import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.parsers.TomlParser
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.fail

/**
 * Runs the [toml-lang/toml-test](https://github.com/toml-lang/toml-test) compliance suite
 * against ktoml's parser (TOML 1.0 file list).
 *
 * - **Valid tests:** parse TOML → convert AST to tagged JSON via [TomlTestConverter] → compare
 *   with expected JSON from the test suite.
 * - **Invalid tests:** verify that parsing throws an exception.
 *
 * Known failures are declared in [TomlTestBaseline.kt]. Every test is still executed:
 * - If a known failure **still fails** → test is skipped (expected behavior).
 * - If a known failure **now passes** → test **FAILS** with an XPASS message prompting
 *   removal from the baseline. This ensures bug fixes are detected automatically.
 *
 * ## Running
 *
 * ```
 * ./gradlew :ktoml-core:jvmTest --tests "com.akuleshov7.ktoml.compliance.TomlTestSuite"
 * ```
 *
 * Requires the `toml-test` git submodule: `git submodule update --init`
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TomlTestSuite {
    private val testDir = File("../toml-test/tests")

    private fun loadFileList(version: String = "1.0.0"): List<String> {
        val listFile = testDir.resolve("files-toml-$version")
        require(listFile.exists()) {
            "toml-test file list not found at ${listFile.absolutePath}. " +
                "Did you initialize the git submodule? Run: git submodule update --init"
        }
        return listFile.readLines().filter { it.isNotBlank() }
    }

    fun validTestCases(): Stream<Arguments> =
        loadFileList()
            .filter { it.startsWith("valid/") && it.endsWith(".toml") }
            .map { Arguments.of(it) }
            .stream()

    fun invalidTestCases(): Stream<Arguments> =
        loadFileList()
            .filter { it.startsWith("invalid/") && it.endsWith(".toml") }
            .map { Arguments.of(it) }
            .stream()

    @ParameterizedTest(name = "valid: {0}")
    @MethodSource("validTestCases")
    fun `valid TOML parses correctly`(testPath: String) {
        val tomlFile = testDir.resolve(testPath)
        val jsonFile = testDir.resolve(testPath.replace(".toml", ".json"))
        assumeTrue(tomlFile.exists(), "TOML file missing: $testPath")
        assumeTrue(jsonFile.exists(), "Expected JSON missing for: $testPath")

        val issueUrl = knownFailuresMap[testPath]

        val result = runCatching {
            val tomlInput = tomlFile.readText()
            val expectedJson = Json.parseToJsonElement(jsonFile.readText())
            val tree = TomlParser(TomlInputConfig.compliant()).parseString(tomlInput)
            val actualJson = TomlTestConverter.toJson(tree)
            assertEquals(expectedJson, actualJson, "Mismatch for $testPath")
        }

        when {
            result.isSuccess && issueUrl != null ->
                fail("XPASS: '$testPath' now passes! Remove it from knownFailures. (Was: $issueUrl)")
            result.isFailure && issueUrl != null ->
                assumeTrue(false, "Known failure: $testPath — $issueUrl")
            result.isFailure ->
                result.getOrThrow()
        }
    }

    @ParameterizedTest(name = "invalid: {0}")
    @MethodSource("invalidTestCases")
    fun `invalid TOML is rejected`(testPath: String) {
        val tomlFile = testDir.resolve(testPath)
        assumeTrue(tomlFile.exists(), "TOML file missing: $testPath")

        val issueUrl = knownFailuresMap[testPath]

        val result = runCatching {
            val tomlInput = tomlFile.readText()
            assertFails("Expected parse failure for $testPath") {
                TomlParser(TomlInputConfig.compliant()).parseString(tomlInput)
            }
        }

        when {
            result.isSuccess && issueUrl != null ->
                fail("XPASS: '$testPath' now passes! Remove it from knownFailures. (Was: $issueUrl)")
            result.isFailure && issueUrl != null ->
                assumeTrue(false, "Known failure: $testPath — $issueUrl")
            result.isFailure ->
                result.getOrThrow()
        }
    }
}

