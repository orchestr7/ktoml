package com.akuleshov7.ktoml.compliance

import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.parsers.TomlParser
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Runs the [toml-lang/toml-test](https://github.com/toml-lang/toml-test) compliance suite
 * against ktoml's parser, using the toml-test file list selected by [loadFileList].
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

    private fun loadFileList(version: String = "1.1.0"): List<String> {
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
        assertTrue(tomlFile.exists(), missingFileMessage(testPath))
        assertTrue(
            jsonFile.exists(),
            "Misconfiguration: expected JSON is listed but missing on disk: " +
                testPath.replace(".toml", ".json"),
        )

        val issueUrl = knownFailuresMap[testPath]

        val result = runCatching {
            val tomlInput = readTomlStrictUtf8(tomlFile)
            val expectedJson = Json.parseToJsonElement(jsonFile.readText())
            val tree = TomlParser(TomlInputConfig.compliant()).parseString(tomlInput)
            val actualJson = TomlTestConverter.toJson(tree)
            assertJsonEqualsWithFloatTolerance(expectedJson, actualJson, testPath)
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
        assertTrue(tomlFile.exists(), missingFileMessage(testPath))

        val issueUrl = knownFailuresMap[testPath]

        val result = runCatching {
            assertFails("Expected parse failure for $testPath") {
                val tomlInput = readTomlStrictUtf8(tomlFile)
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

    /**
     * Guards against stale baseline entries: a path listed in [knownFailuresMap] but absent from the
     * loaded toml-test file list is never executed, so it can neither XPASS nor fail — it just lingers
     * silently and gives a false sense of coverage. Fail loudly so it gets removed.
     */
    @Test
    fun `baseline references only tests present in the file list`() {
        val listed = loadFileList().toSet()
        val stale = knownFailuresMap.keys.filterNot { it in listed }
        assertTrue(
            stale.isEmpty(),
            "Stale baseline entries — listed in TomlTestBaseline.kt but not in the loaded toml-test " +
                "file list, so they never run. Remove them: $stale",
        )
    }

    private fun missingFileMessage(testPath: String) =
        "Misconfiguration: '$testPath' is in the toml-test file list but missing on disk. " +
            "The toml-test submodule is incomplete — run: git submodule update --init --recursive"

    /**
     * Reads a test file with **strict UTF-8** decoding ([ByteArray.decodeToString] with
     * `throwOnInvalidSequence = true`): malformed bytes (a lone `0xC3`, or a surrogate encoded as
     * `ED A0 80`) throw instead of being silently replaced with U+FFFD. This is what lets the suite
     * reject toml-test's `invalid/encoding` cases.
     *
     * This is the *suite's* input boundary, not ktoml behaviour. ktoml-core decodes an
     * already-decoded [String] (no bytes to validate), and ktoml-file/ktoml-source follow okio's
     * lenient UTF-8 reading by design (a real app shouldn't fail on a stray byte). Strict UTF-8 is a
     * standard stdlib flag, applied here only for compliance testing.
     */
    private fun readTomlStrictUtf8(tomlFile: File): String =
        tomlFile.readBytes().decodeToString(throwOnInvalidSequence = true)

    private fun assertJsonEqualsWithFloatTolerance(
        expected: JsonElement,
        actual: JsonElement,
        testPath: String,
        jsonPath: String = "$",
    ) {
        when {
            expected is JsonObject && actual is JsonObject -> {
                if (expected.isTaggedFloat() && actual.isTaggedFloat()) {
                    assertTaggedFloatEquals(expected, actual, testPath, jsonPath)
                    return
                }

                if (expected.isTaggedDateTime() && actual.isTaggedDateTime()) {
                    assertTaggedDateTimeEquals(expected, actual, testPath, jsonPath)
                    return
                }

                assertEquals(expected.keys, actual.keys, "Mismatch for $testPath at $jsonPath")
                expected.keys.forEach { key ->
                    assertJsonEqualsWithFloatTolerance(
                        expected.getValue(key),
                        actual.getValue(key),
                        testPath,
                        "$jsonPath.$key",
                    )
                }
            }

            expected is JsonArray && actual is JsonArray -> {
                assertEquals(expected.size, actual.size, "Mismatch for $testPath at $jsonPath")
                expected.indices.forEach { index ->
                    assertJsonEqualsWithFloatTolerance(
                        expected[index],
                        actual[index],
                        testPath,
                        "$jsonPath[$index]",
                    )
                }
            }

            else -> assertEquals(expected, actual, "Mismatch for $testPath at $jsonPath")
        }
    }

    private fun assertTaggedFloatEquals(
        expected: JsonObject,
        actual: JsonObject,
        testPath: String,
        jsonPath: String,
    ) {
        assertEquals(expected.keys, actual.keys, "Mismatch for $testPath at $jsonPath")
        assertEquals(expected["type"], actual["type"], "Mismatch for $testPath at $jsonPath.type")

        val expectedValue = expected["value"]?.jsonPrimitive?.content
            ?: fail("Missing float value for $testPath at $jsonPath")
        val actualValue = actual["value"]?.jsonPrimitive?.content
            ?: fail("Missing float value for $testPath at $jsonPath")

        if (!floatValuesMatch(expectedValue, actualValue)) {
            fail(
                "Float mismatch for $testPath at $jsonPath.value: " +
                    "expected <$expectedValue> but was <$actualValue>",
            )
        }
    }

    private fun JsonObject.isTaggedFloat(): Boolean =
        (this["type"] as? JsonPrimitive)?.content == "float" && containsKey("value")

    private fun JsonObject.isTaggedDateTime(): Boolean =
        (this["type"] as? JsonPrimitive)?.content in DATETIME_TYPES && containsKey("value")

    /**
     * Compares two tagged date-time values semantically rather than textually.
     *
     * toml-test's reference comparator decodes date-time values and compares the parsed instants,
     * not their textual form. This tolerates representation differences that are all equally valid,
     * e.g. fractional-second precision (`.5` vs `.500`) and offset spelling (`Z` vs `+00:00`).
     * Without this, valid files disagree with each other (`common-27` keeps `.5`, `milliseconds`
     * normalizes `.6` to `.600`), which no single textual emitter could satisfy at once.
     */
    @OptIn(ExperimentalTime::class)
    private fun assertTaggedDateTimeEquals(
        expected: JsonObject,
        actual: JsonObject,
        testPath: String,
        jsonPath: String,
    ) {
        assertEquals(expected.keys, actual.keys, "Mismatch for $testPath at $jsonPath")
        assertEquals(expected["type"], actual["type"], "Mismatch for $testPath at $jsonPath.type")

        val type = (expected["type"] as JsonPrimitive).content
        val expectedValue = expected["value"]?.jsonPrimitive?.content
            ?: fail("Missing datetime value for $testPath at $jsonPath")
        val actualValue = actual["value"]?.jsonPrimitive?.content
            ?: fail("Missing datetime value for $testPath at $jsonPath")

        val expectedKey = canonicalDateTime(type, expectedValue)
        val actualKey = canonicalDateTime(type, actualValue)
        if (expectedKey == null || actualKey == null || expectedKey != actualKey) {
            assertEquals(
                expectedValue,
                actualValue,
                "Datetime mismatch for $testPath at $jsonPath.value",
            )
        }
    }

    /**
     * Normalizes a tagged date-time [value] of the given [type] to a canonical comparable string.
     * Returns `null` if the value cannot be parsed, so the caller can fall back to a textual compare.
     */
    @OptIn(ExperimentalTime::class)
    private fun canonicalDateTime(type: String, value: String): String? = runCatching {
        when (type) {
            // Offset date-times can be spelled differently yet be equal (`Z` vs `+00:00`, `.5` vs
            // `.500`); compare them by the parsed instant via the stdlib (no date library needed).
            "datetime" -> Instant.parse(value).toString()
            // Local values are emitted in canonical form by ktoml, so a plain textual comparison is
            // exact — return null to fall back to it.
            else -> null
        }
    }.getOrNull()

    private fun floatValuesMatch(expected: String, actual: String): Boolean {
        if (expected.isTomlTestNaN() && actual.isTomlTestNaN()) {
            return true
        }

        val expectedFloat = parseTomlTestFloat(expected) ?: return false
        val actualFloat = parseTomlTestFloat(actual) ?: return false
        return expectedFloat == actualFloat
    }

    private fun String.isTomlTestNaN(): Boolean =
        lowercase().removePrefix("+").removePrefix("-") == "nan"

    private fun parseTomlTestFloat(value: String): Double? = when (value.lowercase()) {
        "nan", "+nan", "-nan" -> Double.NaN
        "inf", "+inf" -> Double.POSITIVE_INFINITY
        "-inf" -> Double.NEGATIVE_INFINITY
        else -> value.toDoubleOrNull()
    }

    private companion object {
        private val DATETIME_TYPES = setOf("datetime", "datetime-local", "date-local", "time-local")
    }
}

