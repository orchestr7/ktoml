package com.akuleshov7.ktoml.compliance

/**
 * Known failures for the [toml-lang/toml-test](https://github.com/toml-lang/toml-test) compliance suite.
 *
 * Each [KnownFailure] object declares an issue ID and a list of test paths that fail due to that issue.
 * When a bug is fixed, the test will start passing and [TomlTestSuite] will fail with an
 * XPASS message — remove the test path from the corresponding object.
 *
 * ## Failure categories
 *
 * | Category | Issue |
 * |----------|-------|
 * | Datetime offset normalized to UTC | [#375](https://github.com/orchestr7/ktoml/issues/375) |
 * | Float representation lost | [#376](https://github.com/orchestr7/ktoml/issues/376) |
 * | Dotted key expansion incorrect | [#377](https://github.com/orchestr7/ktoml/issues/377) |
 * | Array-of-tables structure | [#378](https://github.com/orchestr7/ktoml/issues/378) |
 * | Key names not unquoted | [#379](https://github.com/orchestr7/ktoml/issues/379) |
 * | Parser rejects valid TOML | [#380](https://github.com/orchestr7/ktoml/issues/380) |
 * | Stack overflow on deep nesting | [#381](https://github.com/orchestr7/ktoml/issues/381) |
 * | Multiline string escape handling | [#382](https://github.com/orchestr7/ktoml/issues/382) |
 * | Multiline inline table crash | [#374](https://github.com/orchestr7/ktoml/issues/374) |
 * | Missing validation (accepts invalid) | [#383](https://github.com/orchestr7/ktoml/issues/383) |
 * | TOML 1.1 valid features unsupported | [#373](https://github.com/orchestr7/ktoml/issues/373) |
 *
 * The suite runs against the **TOML 1.1** file list (`files-toml-1.1.0`), matching the project goal.
 *
 * Related: [#32](https://github.com/orchestr7/ktoml/issues/32) (toml-test integration),
 * [#373](https://github.com/orchestr7/ktoml/issues/373) (TOML 1.1 umbrella)
 */
sealed interface KnownFailure {
    val issue: Int
    val tests: List<String>

    val issueUrl: String get() = "https://github.com/orchestr7/ktoml/issues/$issue"
}

/** Offset datetimes normalized to UTC, losing original timezone */
data object DatetimeOffsetLoss : KnownFailure {
    override val issue = 375
    override val tests = listOf(
        "valid/comment/everywhere.toml",
        "valid/datetime/milliseconds.toml",
        "valid/datetime/timezone.toml",
        "valid/spec-1.0.0/offset-date-time-0.toml",
        "valid/spec-example-1.toml",
        "valid/spec-example-1-compact.toml",
    )
}

/** Scientific notation lost after parsing to Double */
data object FloatRepresentationLoss : KnownFailure {
    override val issue = 376
    override val tests = listOf(
        "valid/float/exponent.toml",
        "valid/spec-1.0.0/float-0.toml",
    )
}

/** Dotted keys don't always create correct synthetic nested tables */
data object DottedKeyExpansion : KnownFailure {
    override val issue = 377
    override val tests = listOf(
        "valid/inline-table/key-dotted-01.toml",
        "valid/inline-table/key-dotted-02.toml",
        "valid/inline-table/key-dotted-04.toml",
        "valid/inline-table/key-dotted-06.toml",
        "valid/key/dotted-01.toml",
        "valid/key/dotted-02.toml",
        "valid/key/dotted-04.toml",
        "valid/key/dotted-empty.toml",
        "valid/key/quoted-dots.toml",
    )
}

/** Array-of-tables produces wrong AST structure in edge cases */
data object ArrayOfTablesStructure : KnownFailure {
    override val issue = 378
    override val tests = listOf(
        "valid/array/open-parent-table.toml",
        "valid/table/array-empty-name.toml",
        "valid/table/array-implicit.toml",
        "valid/table/array-implicit-and-explicit-after.toml",
        "valid/table/array-table-array.toml",
    )
}

/** Key names retain quote characters in AST name property */
data object KeyNameQuoting : KnownFailure {
    override val issue = 379
    override val tests = listOf(
        "valid/key/space.toml",
        "valid/multibyte.toml",
        "valid/spec-1.0.0/table-3.toml",
        "valid/table/empty-name.toml",
        "valid/table/with-literal-string.toml",
        "valid/table/with-single-quotes.toml",
    )
}

/** ktoml rejects valid TOML with ParseException */
data object ValidTomlRejected : KnownFailure {
    override val issue = 380
    override val tests = listOf(
        "valid/array/mixed-string-table.toml",
        "valid/datetime/edge.toml",
        "valid/datetime/leap-year.toml",
        "valid/datetime/local.toml",
        "valid/float/max-int.toml",
        "valid/float/underscore.toml",
        "valid/inline-table/key-dotted-05.toml",
        "valid/inline-table/nest.toml",
        "valid/key/escapes.toml",
        "valid/key/quoted-unicode.toml",
        "valid/key/special-chars.toml",
        "valid/spec-1.0.0/array-0.toml",
        "valid/spec-1.0.0/float-1.toml",
        "valid/spec-1.0.0/keys-1.toml",
        "valid/table/names.toml",
        "valid/table/names-with-values.toml",
    )
}

/** Stack overflow on deeply nested structures */
data object StackOverflowOnNesting : KnownFailure {
    override val issue = 381
    override val tests = listOf(
        "valid/array/nested-double.toml",
        "valid/comment/tricky.toml",
    )
}

/** Line-ending backslash in multiline strings not handled correctly */
data object MultilineStringEscape : KnownFailure {
    override val issue = 382
    override val tests = listOf(
        "valid/string/ends-in-whitespace-escape.toml",
        "valid/string/multiline.toml",
        "valid/string/multiline-empty.toml",
    )
}

/** Multiline inline tables crash with "character count -1" */
data object MultilineInlineTableCrash : KnownFailure {
    override val issue = 374
    override val tests = listOf(
        "valid/inline-table/array-02.toml",
        "valid/inline-table/array-03.toml",
        "valid/inline-table/key-dotted-07.toml",
        "valid/inline-table/multiline.toml",
        "valid/inline-table/newline.toml",
        "valid/inline-table/newline-comment.toml",
    )
}

/** Missing validation — control characters not rejected */
data object MissingValidationControlChars : KnownFailure {
    override val issue = 383
    override val tests = listOf(
        "invalid/control/bare-cr.toml",
        "invalid/control/comment-cr.toml",
        "invalid/control/comment-del.toml",
        "invalid/control/comment-ff.toml",
        "invalid/control/comment-lf.toml",
        "invalid/control/comment-null.toml",
        "invalid/control/comment-us.toml",
        "invalid/control/multi-cr.toml",
        "invalid/control/multi-del.toml",
        "invalid/control/multi-lf.toml",
        "invalid/control/multi-null.toml",
        "invalid/control/multi-us.toml",
        "invalid/control/only-ff.toml",
        "invalid/control/only-vt.toml",
        "invalid/control/rawmulti-cr.toml",
        "invalid/control/rawmulti-del.toml",
        "invalid/control/rawmulti-lf.toml",
        "invalid/control/rawmulti-null.toml",
        "invalid/control/rawmulti-us.toml",
        "invalid/control/rawstring-cr.toml",
        "invalid/control/rawstring-del.toml",
        "invalid/control/rawstring-lf.toml",
        "invalid/control/rawstring-null.toml",
        "invalid/control/rawstring-us.toml",
        "invalid/control/string-bs.toml",
        "invalid/control/string-cr.toml",
        "invalid/control/string-del.toml",
        "invalid/control/string-lf.toml",
        "invalid/control/string-null.toml",
        "invalid/control/string-us.toml",
    )
}

/** Missing validation — bad UTF-8 encoding not rejected */
data object MissingValidationEncoding : KnownFailure {
    override val issue = 383
    override val tests = listOf(
        "invalid/encoding/bad-codepoint.toml",
        "invalid/encoding/bad-utf8-in-comment.toml",
        "invalid/encoding/bad-utf8-in-multiline.toml",
        "invalid/encoding/bad-utf8-in-multiline-literal.toml",
        "invalid/encoding/bad-utf8-in-string.toml",
        "invalid/encoding/bad-utf8-in-string-literal.toml",
        "invalid/encoding/ideographic-space.toml",
    )
}

/** Missing validation — table redefinition and duplicate keys not rejected */
data object MissingValidationTableRedefinition : KnownFailure {
    override val issue = 383
    override val tests = listOf(
        "invalid/table/append-with-dotted-keys-01.toml",
        "invalid/table/append-with-dotted-keys-02.toml",
        "invalid/table/append-with-dotted-keys-03.toml",
        "invalid/table/append-with-dotted-keys-04.toml",
        "invalid/table/append-with-dotted-keys-05.toml",
        "invalid/table/append-with-dotted-keys-06.toml",
        "invalid/table/append-with-dotted-keys-07.toml",
        "invalid/table/append-with-dotted-keys-08.toml",
        "invalid/table/array-implicit.toml",
        "invalid/table/dot.toml",
        "invalid/table/dotdot.toml",
        "invalid/table/duplicate-key-01.toml",
        "invalid/table/duplicate-key-02.toml",
        "invalid/table/duplicate-key-03.toml",
        "invalid/table/duplicate-key-04.toml",
        "invalid/table/duplicate-key-05.toml",
        "invalid/table/duplicate-key-06.toml",
        "invalid/table/duplicate-key-07.toml",
        "invalid/table/duplicate-key-08.toml",
        "invalid/table/duplicate-key-09.toml",
        "invalid/table/duplicate-key-10.toml",
        "invalid/table/empty-implicit-table.toml",
        "invalid/table/multiline-key-01.toml",
        "invalid/table/multiline-key-02.toml",
        "invalid/table/overwrite-array-in-parent.toml",
        "invalid/table/overwrite-bool-with-array.toml",
        "invalid/table/overwrite-with-deep-table.toml",
        "invalid/table/redefine-01.toml",
        "invalid/table/redefine-02.toml",
        "invalid/table/redefine-03.toml",
        "invalid/table/super-twice.toml",
        "invalid/table/trailing-dot.toml",
    )
}

/** Missing validation — malformed integer literals not rejected */
data object MissingValidationIntegerFormat : KnownFailure {
    override val issue = 383
    override val tests = listOf(
        "invalid/integer/double-us.toml",
        "invalid/integer/invalid-hex-03.toml",
        "invalid/integer/leading-us.toml",
        "invalid/integer/leading-us-bin.toml",
        "invalid/integer/leading-us-hex.toml",
        "invalid/integer/leading-us-oct.toml",
        "invalid/integer/leading-zero-01.toml",
        "invalid/integer/leading-zero-02.toml",
        "invalid/integer/leading-zero-03.toml",
        "invalid/integer/leading-zero-sign-01.toml",
        "invalid/integer/leading-zero-sign-02.toml",
        "invalid/integer/leading-zero-sign-03.toml",
        "invalid/integer/trailing-us.toml",
        "invalid/integer/trailing-us-bin.toml",
        "invalid/integer/trailing-us-hex.toml",
        "invalid/integer/trailing-us-oct.toml",
        "invalid/integer/us-after-bin.toml",
        "invalid/integer/us-after-hex.toml",
        "invalid/integer/us-after-oct.toml",
    )
}

/** Missing validation — malformed float literals not rejected */
data object MissingValidationFloatFormat : KnownFailure {
    override val issue = 383
    override val tests = listOf(
        "invalid/float/exp-dot-02.toml",
        "invalid/float/exp-dot-03.toml",
        "invalid/float/leading-dot.toml",
        "invalid/float/leading-dot-neg.toml",
        "invalid/float/leading-dot-plus.toml",
        "invalid/float/leading-zero.toml",
        "invalid/float/leading-zero-neg.toml",
        "invalid/float/leading-zero-plus.toml",
        "invalid/float/nan-capital.toml",
        "invalid/float/trailing-dot.toml",
        "invalid/float/trailing-dot-01.toml",
        "invalid/float/trailing-dot-02.toml",
        "invalid/float/trailing-dot-min.toml",
        "invalid/float/trailing-dot-plus.toml",
    )
}

/** Missing validation — invalid keys not rejected */
data object MissingValidationKeys : KnownFailure {
    override val issue = 383
    override val tests = listOf(
        "invalid/key/dot.toml",
        "invalid/key/dotdot.toml",
        "invalid/key/dotted-redefine-table-01.toml",
        "invalid/key/dotted-redefine-table-02.toml",
        "invalid/key/duplicate-keys-01.toml",
        "invalid/key/duplicate-keys-02.toml",
        "invalid/key/duplicate-keys-03.toml",
        "invalid/key/duplicate-keys-04.toml",
        "invalid/key/duplicate-keys-05.toml",
        "invalid/key/duplicate-keys-07.toml",
        "invalid/key/duplicate-keys-08.toml",
        "invalid/key/duplicate-keys-09.toml",
        "invalid/key/multiline-key-01.toml",
        "invalid/key/multiline-key-02.toml",
        "invalid/key/multiline-key-03.toml",
        "invalid/key/multiline-key-04.toml",
        "invalid/key/partial-quoted.toml",
        "invalid/key/start-dot.toml",
    )
}

/** Missing validation — invalid inline tables not rejected */
data object MissingValidationInlineTable : KnownFailure {
    override val issue = 383
    override val tests = listOf(
        "invalid/inline-table/duplicate-key-01.toml",
        "invalid/inline-table/duplicate-key-02.toml",
        "invalid/inline-table/duplicate-key-03.toml",
        "invalid/inline-table/duplicate-key-04.toml",
        "invalid/inline-table/overwrite-01.toml",
        "invalid/inline-table/overwrite-02.toml",
        "invalid/inline-table/overwrite-03.toml",
        "invalid/inline-table/overwrite-04.toml",
        "invalid/inline-table/overwrite-05.toml",
        "invalid/inline-table/overwrite-06.toml",
        "invalid/inline-table/overwrite-07.toml",
        "invalid/inline-table/overwrite-08.toml",
        "invalid/inline-table/overwrite-09.toml",
        "invalid/inline-table/overwrite-10.toml",
        "invalid/spec-1.0.0/inline-table-2-0.toml",
        "invalid/spec-1.0.0/inline-table-3-0.toml",
        "invalid/spec-1.0.0/table-9-0.toml",
        "invalid/spec-1.0.0/table-9-1.toml",
    )
}

/** Missing validation — invalid string escapes not rejected */
data object MissingValidationStringEscape : KnownFailure {
    override val issue = 383
    override val tests = listOf(
        "invalid/string/bad-escape-03.toml",
        "invalid/string/bad-uni-esc-06.toml",
        "invalid/string/bad-uni-esc-ml-06.toml",
        "invalid/string/multiline-bad-escape-04.toml",
    )
}

/** Missing validation — invalid datetimes not rejected */
data object MissingValidationDatetime : KnownFailure {
    override val issue = 383
    override val tests = listOf(
        "invalid/datetime/offset-minus-no-minute.toml",
        "invalid/datetime/offset-plus-no-minute.toml",
        "invalid/datetime/second-trailing-dot.toml",
        "invalid/local-time/no-secs.toml",
        "invalid/local-time/trailing-dot.toml",
        "invalid/local-datetime/no-secs.toml",
    )
}

/** Missing validation — invalid arrays not rejected */
data object MissingValidationArrays : KnownFailure {
    override val issue = 383
    override val tests = listOf(
        "invalid/array/extend-defined-aot.toml",
        "invalid/array/extending-table.toml",
        "invalid/array/only-comma-01.toml",
        "invalid/array/tables-01.toml",
        "invalid/array/tables-02.toml",
    )
}

/**
 * TOML 1.1 valid features ktoml does not yet support (parser rejects or mis-represents them).
 * Tracked under the TOML 1.1 umbrella [#373](https://github.com/orchestr7/ktoml/issues/373):
 * `\x` / `\e` string escapes, seconds-less datetimes, and assorted 1.1 spec examples.
 */
data object TomlOneOneValidFeatures : KnownFailure {
    override val issue = 373
    override val tests = listOf(
        "valid/string/hex-escape.toml",
        "valid/string/escape-esc.toml",
        "valid/datetime/no-seconds.toml",
        "valid/spec-1.1.0/common-4.toml",
        "valid/spec-1.1.0/common-12.toml",
        "valid/spec-1.1.0/common-23.toml",
        "valid/spec-1.1.0/common-24.toml",
        "valid/spec-1.1.0/common-27.toml",
        "valid/spec-1.1.0/common-29.toml",
        "valid/spec-1.1.0/common-35.toml",
        "valid/spec-1.1.0/common-40.toml",
        "valid/spec-1.1.0/common-47.toml",
    )
}

/** Missing validation — TOML 1.1 invalid spec examples that ktoml accepts. Tracked under #373/#383. */
data object TomlOneOneMissingValidation : KnownFailure {
    override val issue = 373
    override val tests = listOf(
        "invalid/spec-1.1.0/common-46-0.toml",
        "invalid/spec-1.1.0/common-46-1.toml",
        "invalid/spec-1.1.0/common-49-0.toml",
        "invalid/spec-1.1.0/common-50-0.toml",
    )
}

/**
 * All known failure groups. Used by [TomlTestSuite] to build the lookup map.
 */
val allKnownFailures: List<KnownFailure> = listOf(
    DatetimeOffsetLoss,
    FloatRepresentationLoss,
    DottedKeyExpansion,
    ArrayOfTablesStructure,
    KeyNameQuoting,
    ValidTomlRejected,
    StackOverflowOnNesting,
    MultilineStringEscape,
    MultilineInlineTableCrash,
    MissingValidationControlChars,
    MissingValidationEncoding,
    MissingValidationTableRedefinition,
    MissingValidationIntegerFormat,
    MissingValidationFloatFormat,
    MissingValidationKeys,
    MissingValidationInlineTable,
    MissingValidationStringEscape,
    MissingValidationDatetime,
    MissingValidationArrays,
    TomlOneOneValidFeatures,
    TomlOneOneMissingValidation,
)

/**
 * Combined map of all known failures: test path → issue URL.
 * Used by [TomlTestSuite] to skip expected failures and detect fixes.
 */
val knownFailuresMap: Map<String, String> by lazy {
    allKnownFailures.flatMap { failure ->
        failure.tests.map { it to failure.issueUrl }
    }.toMap()
}
