# TOML 1.1 compatibility

This change retains the upstream Kotlin, serialization and kotlinx-datetime versions and keeps
kotlinx-datetime as a transitive core dependency. Existing date-time emitter overloads remain available.

Parser AST helpers changed to support strict key/table validation. Applications using these helpers
may need to recompile and adapt their calls. Normal string encode/decode entry points are retained.

The existing six-Boolean TomlInputConfig constructor, copy and their default-argument JVM entry
points are retained. Tests run Java/Kotlin consumers compiled against the published release to check
those configuration entry points; this does not establish binary compatibility for the entire AST.
