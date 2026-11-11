# API and binary compatibility

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
  requires internal opt-in.
* AST insertion and inline-table helpers changed signatures. Recompile custom AST consumers
  and adapt calls to the current declarations.


The six-Boolean `TomlInputConfig` constructor, `copy` and their default-argument JVM entry points
are retained with hidden bridges. Five tests execute Java/Kotlin bytecode compiled against the
published 0.7.1 jar, without recompiling it against this source. They cover those configuration
entry points only. These tests cover configuration compatibility rather than the entire library.

