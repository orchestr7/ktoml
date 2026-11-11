package com.akuleshov7.ktoml.compliance

/**
 * Known failures for the [toml-lang/toml-test](https://github.com/toml-lang/toml-test) compliance suite —
 * test paths that [TomlTestSuite] expects to fail because of an open ktoml bug or a deliberate gap.
 *
 * Each [KnownFailure] object declares an issue ID and the failing test paths. When a fix lands the
 * test starts passing and [TomlTestSuite] fails with an XPASS message — remove the path from its object.
 *
 * **There are currently no known failures: the whole suite passes.** In particular, the
 * `invalid/encoding` cases (malformed UTF-8) are rejected at the suite's strict-UTF-8 input
 * boundary — see `TomlTestSuite.readTomlStrictUtf8`. That is a property of how the suite reads bytes,
 * not of ktoml: ktoml-core decodes an already-decoded [String] (no bytes to validate), and
 * ktoml-file/ktoml-source follow okio's lenient UTF-8 reading by design.
 *
 * The suite is pinned by the `toml-test` git submodule.
 */
sealed interface KnownFailure {
    val issue: Int
    val tests: List<String>

    val issueUrl: String get() = "https://github.com/orchestr7/ktoml/issues/$issue"
}

/**
 * All known failure groups, used by [TomlTestSuite] to build the lookup map. Empty — add a
 * [KnownFailure] object here only when a genuine ktoml spec gap is found (and remove it once fixed).
 */
val allKnownFailures: List<KnownFailure> = emptyList()

/**
 * Combined map of all known failures: test path → issue URL.
 * Used by [TomlTestSuite] to skip expected failures and detect fixes.
 */
val knownFailuresMap: Map<String, String> by lazy {
    allKnownFailures.flatMap { failure ->
        failure.tests.map { it to failure.issueUrl }
    }.toMap()
}
