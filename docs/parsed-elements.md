# Decoding a parsed document

`partiallyDecodeFromString` parses the input on every call. To decode several tables from the same
input, parse it once and retain the document or table elements.

## Experimental elements

`TomlElement` exposes `kind`, `keys()` and `tables()` without exposing the mutable parser AST.
The API requires `ExperimentalKtomlApi` opt-in.

```kotlin
@file:OptIn(com.akuleshov7.ktoml.annotations.ExperimentalKtomlApi::class)

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.decodeFromTomlElement
import kotlinx.serialization.Serializable

@Serializable
data class Server(val port: Int)

val document = Toml.parseToTomlElement("[server]\nport = 8080")
val server = Toml.decodeFromTomlElement<Server>(document.tables().getValue("server"))
```

Keep `document` to select and decode more tables without parsing again.

## Lower-level AST access

Code that needs the mutable AST can use `tomlParser` and `decodeFromTomlNode`. This also requires
`InternalKtomlApi` opt-in: the AST is an internal API and may change.

For example, you can build a map keyed by each explicit table's full path. The tables you select
must all match the type being decoded; ktoml does not flatten the document automatically.

```kotlin
@file:OptIn(
    com.akuleshov7.ktoml.annotations.ExperimentalKtomlApi::class,
    com.akuleshov7.ktoml.annotations.InternalKtomlApi::class,
)

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.decodeFromTomlNode
import kotlinx.serialization.Serializable

@Serializable
data class Foobar(val foo: String, val bar: String)

val input = """
    [a]
    foo = "one"
    bar = "two"
    [b.a]
    foo = "three"
    bar = "four"
""".trimIndent()
val file = Toml.tomlParser.parseString(input)
val byPath: Map<String, Foobar> = file.getRealTomlTables().associate { table ->
    table.fullTableKey.toString() to Toml.decodeFromTomlNode<Foobar>(table)
}
// byPath has keys "a" and "b.a".

val table = file.findTableInAstByName("b.a")!!
val decoded = Toml.decodeFromTomlNode<Foobar>(table)
```

Use the element API when you only need to select and decode tables. Use the internal AST only when
its additional access is needed.
