# API and binary compatibility

## Kotlin compiler compatibility

Keep the consumer compiler requirement as old as the serialization runtime permits. Do not raise
it just to follow a newer Kotlin compiler release; a serialization upgrade requiring newer
metadata must be verified and documented before changing the consumer baseline.

The library is built with upstream's Kotlin 2.4.21 and serialization 1.12.0. This does not mean
every consumer needs compiler 2.4.21. `checkJvmConsumerCompatibility` independently resolves
Kotlin **2.3.0** and its matching serialization compiler plugin, compiles a real application
against the integrated JVM jar and its transitive runtime dependencies, and executes a TOML 1.1
decode/encode round trip using its generated serializers. It runs as part of `check`/`build` and CI.

```sh
./gradlew checkJvmConsumerCompatibility
```

Kotlin 2.2.0 was also tested: it rejects serialization 1.12.0's JVM metadata version 2.4.0
(the compiler accepts up to 2.3.0). Its Native compiler rejects the serialization-only control
because that dependency has ABI version 2.4.0, whereas the compiler accepts up to 2.2.0.
Reducing ktoml's build compiler alone cannot restore Kotlin 2.2 compatibility with this runtime.
No metadata-version bypass flags are used. The JVM result is not a compatibility claim for older
Native/JS/Wasm compilers; those must support the serialization runtime's KLib ABI.

## Integration and migration

This TOML 1.1 integration retains the `com.akuleshov7` Maven group and Kotlin packages. It includes
API and dependency changes; it is not a drop-in binary replacement for published upstream 0.7.1.
Recompile consumers, including libraries that expose or use ktoml types, and review these changes:

* `ktoml-core` no longer brings in `kotlinx-datetime`. Add it explicitly when using
  `kotlinx.datetime.LocalDate`, `LocalDateTime` or `LocalTime`. Offset date-times use
  `kotlin.time.Instant`; strings can represent all four TOML date-time kinds.
* The AST preserves date-time text in `TomlOffsetDateTime`, `TomlLocalDateTime`, `TomlLocalDate`
  and `TomlLocalTime`. `TomlDateTime.content` and `TomlEmitter.emitValue` use these representations.
  Emitter overloads accepting concrete date/time types are replaced by these wrappers, including
  the `kotlin.time.Instant` overload present on current upstream main.
* AST/parser/encoder utilities marked `@InternalKtomlApi` now require explicit opt-in. These
  signatures are not stable contracts. Subclassing `Toml`, `TomlEmitter` or `TomlWriter` also
  requires internal opt-in. `TomlElement`, node/element decoding and the new I/O
  adapters are experimental and require `@OptIn(ExperimentalKtomlApi::class)`.
* AST insertion and inline-table helpers changed signatures. Recompile custom AST consumers
  and adapt calls to the current declarations. Prefer `Toml.parseToTomlElement` and
  `decodeFromTomlElement` for new tree consumers.
* `ktoml-file` and `ktoml-source` remain available as deprecated compatibility adapters. New
  applications can use `ktoml-okio` or `ktoml-kotlinx-io`. Caller-owned streams remain open;
  adapters do not flush them.

The six-Boolean `TomlInputConfig` constructor, `copy` and their default-argument JVM entry points
are retained with hidden bridges. Five tests execute Java/Kotlin bytecode compiled against the
published 0.7.1 jar, without recompiling it against this source. They cover those configuration
entry points only. Hidden/synthetic methods can be absent from API dumps, so the consumer tests
complement the checks below.

## Source API baseline

The binary compatibility validator compares compiled JVM and KLib APIs to committed dumps for
`ktoml-core`, `ktoml-file`, `ktoml-source`, `ktoml-okio` and `ktoml-kotlinx-io`.
`ktoml-datetime-tests` is a test consumer and is excluded. Public declarations carrying internal
or experimental markers remain in the baseline: there are no marker/package exclusions.

The baseline originates from [the fork's API validation work](https://github.com/gildor/ktoml/pull/52),
reconciled with Kotlin 2.4.21 and serialization 1.12.0. The validator is version 0.18.1. Relative
to that baseline, only the restored six-Boolean config constructor/copy entries and their JVM
default bridges are added; all other recorded signatures are retained. It records the integrated
source API for review of future changes. A passing check
does not establish compatibility with a published release, dependency ABI compatibility, or
runtime behavior on another platform. Additions also produce differences because dumps use
equality.

```sh
./gradlew apiCheck

# CI requires actual compiled APIs for every configured target, including other hosts.
./gradlew apiCheck -PapiStrictKlibValidation=true -Pkotlin.native.enableKlibsCrossCompilation=true
```

Core covers JVM, JS, Wasm JS/WASI and Native; the adapters cover JVM and Native. Native targets
are Linux arm64/x64, MinGW x64, macOS arm64/x64 and iOS arm64/x64/simulator arm64. Ordinary local
checks can skip unavailable targets. Strict validation rejects skipped targets; cross-compilation
checks library signatures without executing the target's tests.

The validator attaches `apiCheck` to `check`/`build`. CI and the existing release workflow also
run strict API checks explicitly. CI does not generate or update dumps. For a deliberate API
change, run `apiDump` with all targets available, inspect every difference, and commit the
reviewed dumps together with the change. Regeneration does not resolve a compatibility regression.

## Published-artifact diagnostics

These independent, opt-in tasks resolve published core artifacts non-transitively from Maven
Central and dump them using the same validator as the source baseline:

```sh
./gradlew :ktoml-core:referenceJvmApiDiff
./gradlew :ktoml-core:referenceJsApiDiff

# Override the reference without changing the library's dependencies.
./gradlew :ktoml-core:referenceJvmApiDiff -PreferenceApiCoordinate=com.akuleshov7:ktoml-core-jvm:0.7.1
./gradlew :ktoml-core:referenceJsApiDiff -PreferenceJsApiCoordinate=com.akuleshov7:ktoml-core-js:0.7.1@klib
```

Defaults are the upstream 0.7.1 JVM jar and JS KLib. `referenceJvmApiDump` and `referenceJsApiDump`
write the reference alone; generated files are under `ktoml-core/build/api/reference/`.
The diff tasks exit nonzero on any difference, including additions, without allowlists. They
are diagnostics, separate from the source baseline and normal `check`/publishing. Their generic
`apiDump` suggestion does not repair a difference against a published artifact.

Known differences include the AST and emitter changes above. The configuration bridges are
tested separately. JVM/JS diagnostics do not establish compatibility for published adapters,
Native/Wasm KLibs, JS exports or runtime linking. This integration keeps upstream KLib group
identity; it does not import the fork's `app.gildor` publication identity.

An independent Java 8 emitter consumer was compiled against the published upstream 0.7.1 jar
(SHA-256 `c22cb661eefc75e30007a1b08d37c4e93a157f7f3a950a478251632c0af6050d`). Its
`emitValue(new kotlinx.datetime.LocalDate(2026, 11, 10))` call succeeds on that jar and on upstream
main at `1fc9e8be245e43d8929159492f7cae7e55b9f433`, but throws `NoSuchMethodError` on this
integration. The date library was present in all three runs. This verifies an actual binary
break introduced by the date-library separation; it is not merely an API-dump difference.

## Checking the checker

Temporarily change `TomlInputConfig.ignoreUnknownNames` from `public val` to `internal val`.
The library must compile but both JVM and KLib API checks must fail on the removed public
getter/property. Restore the source without regenerating dumps and rerun strict `apiCheck`.
This tests signature detection independently of a deliberately corrupted golden file.

During integration, both published upstream audits produced signature diffs. The same diagnostics
passed when pointed at the integrated JVM jar and JS KLib themselves. Removing the public
`ignoreUnknownNames` getter compiled successfully and failed both JVM and KLib API checks; the
source was restored with all ten baseline file hashes unchanged.
