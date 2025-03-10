## <img src="/ktoml.png" width="300px"/>

[![Releases](https://img.shields.io/github/v/release/akuleshov7/ktoml)](https://github.com/orchestr7/ktoml/releases)
[![Maven Central](https://img.shields.io/maven-central/v/com.akuleshov7/ktoml-core)](https://search.maven.org/artifact/com.akuleshov7/ktoml-core/)
[![License](https://img.shields.io/github/license/akuleshov7/ktoml)](https://github.com/orchestr7/ktoml/blob/main/LICENSE)
![Build and test](https://github.com/akuleshov7/ktoml/actions/workflows/build_and_test.yml/badge.svg?branch=main)
![Lines of code](https://img.shields.io/tokei/lines/github/akuleshov7/ktoml)
![Hits-of-Code](https://hitsofcode.com/github/akuleshov7/ktoml?branch=main)
![GitHub repo size](https://img.shields.io/github/repo-size/akuleshov7/ktoml)
![codebeat badge](https://codebeat.co/badges/0518ea49-71ed-4bfd-8dd3-62da7034eebd)
![maintainability](https://api.codeclimate.com/v1/badges/c75d2d6b0d44cea7aefe/maintainability)
![Run deteKT](https://github.com/akuleshov7/ktoml/actions/workflows/detekt.yml/badge.svg?branch=main)
![Run diKTat](https://github.com/akuleshov7/ktoml/actions/workflows/diktat.yml/badge.svg?branch=main)

Fully Native and Multiplatform Kotlin serialization library for serialization/deserialization of [toml](https://toml.io/en/) format.
Uses native [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization), provided by Kotlin. This library contains no Java code and no Java dependencies.
We believe that TOML is actually the most readable and user-friendly **configuration file** format.
So we decided to support this format for the `kotlinx` serialization library.

## Contribution
As this project [is needed](https://github.com/Kotlin/kotlinx.serialization/issues/1092) by the Kotlin community, we need your help.
We will be glad if you will test `ktoml` or contribute to this project.
In case you don't have much time for this - at least spend 5 seconds to give us a star to attract other contributors!

**Thanks!** :pray: :partying_face:

## Acknowledgement
Special thanks to those awesome developers who give us great suggestions, help us to maintain and improve this project:
@NightEule5, @bishiboosh, @Peanuuutz, @petertrr, @nulls, @Olivki, @edrd-f, @BOOMeranGG, @aSemy, @thomasgalvin

## Supported platforms
All the code is written in Kotlin **common** module. This means that it can be built for each and every Kotlin native platform.
However, to reduce the scope, ktoml now supports only the following platforms:
- jvm
- mingwx64
- linuxx64
- macosx64
- macosArm64 (M1)
- ios
- iosSimulatorArm64
- js (obviously only for ktoml-core!). Note, that `js(LEGACY)` is [not supported](https://github.com/Kotlin/kotlinx.serialization/issues/1448)

Other platforms could be added later on the demand (just create a corresponding issue) or easily built by users on their machines.

:globe_with_meridians: ktoml supports Kotlin 1.9.22

## Current limitations
:heavy_exclamation_mark: Please note, that TOML standard does not define Java-like types: `Char`, `Short`, etc. 
You can check types that are supported in TOML standard [here](https://toml.io/en/v1.0.0#string).
However, in Ktoml, our goal is to comprehensively support all primitive types offered by Kotlin.

**General** \
We are still developing and testing this library, so it has several limitations: \
:white_check_mark: deserialization (with some parsing limitations) \
:white_check_mark: serialization (with tree-related limitations)

**Parsing and decoding** \
:white_check_mark: Table sections (single and dotted) \
:white_check_mark: Key-value pairs (single and dotted) \
:white_check_mark: Long/Integer/Byte/Short types \
:white_check_mark: Double/Float types \
:white_check_mark: Basic Strings \
:white_check_mark: Literal Strings \
:white_check_mark: Char type \
:white_check_mark: Boolean type \
:white_check_mark: Simple Arrays \
:white_check_mark: Comments \
:white_check_mark: Inline Tables \
:white_check_mark: Offset Date-Time (to `Instant` of [kotlinx-datetime](https://github.com/Kotlin/kotlinx-datetime)) \
:white_check_mark: Local Date-Time (to `LocalDateTime` of [kotlinx-datetime](https://github.com/Kotlin/kotlinx-datetime)) \
:white_check_mark: Local Date (to `LocalDate` of [kotlinx-datetime](https://github.com/Kotlin/kotlinx-datetime)) \
:white_check_mark: Local Time (to `LocalTime` of [kotlinx-datetime](https://github.com/Kotlin/kotlinx-datetime)) \
:white_check_mark: Multiline Strings \
:white_check_mark: Arrays (including multiline and nested arrays) \
:white_check_mark: Maps (for anonymous key-value pairs) \
:x: Arrays: of Different Types \
:x: Nested Inline Tables \
:x: Array of Tables \
:x: Inline Array of Tables

## Dependency
The library is hosted on the [Maven Central](https://search.maven.org/artifact/com.akuleshov7/ktoml-core).
To import `ktoml` library you need to add following dependencies to your code:
<details>
<summary>Maven</summary>

```pom
<dependency>
  <groupId>com.akuleshov7</groupId>
  <artifactId>ktoml-core</artifactId>
  <version>0.5.1</version>
</dependency>
<dependency>
  <groupId>com.akuleshov7</groupId>
  <artifactId>ktoml-file</artifactId>
  <version>0.5.1</version>
</dependency>
```
</details>

<details>
<summary>Gradle Groovy</summary>

```groovy
implementation 'com.akuleshov7:ktoml-core:0.5.1'
implementation 'com.akuleshov7:ktoml-file:0.5.1'
```
</details>

<details>
<summary>Gradle Kotlin</summary>

```kotlin
implementation("com.akuleshov7:ktoml-core:0.5.1")
implementation("com.akuleshov7:ktoml-file:0.5.1")
```
</details>

## How to use
:heavy_exclamation_mark: as TOML is a foremost language for config files, we have also supported the deserialization from file.
However, we are using [okio](https://github.com/square/okio) to read the file, so it will be added as a dependency to your
project if you will import [ktoml-file](https://search.maven.org/artifact/com.akuleshov7/ktoml-file). 
Same about okio `Source` (for example if you need Streaming): [ktoml-source](https://search.maven.org/artifact/com.akuleshov7/ktoml-source).
For basic scenarios of decoding strings you can simply use [ktoml-core](https://search.maven.org/artifact/com.akuleshov7/ktoml-core).

:heavy_exclamation_mark: don't forget to add the serialization plugin `kotlin("plugin.serialization")` to your project.
Otherwise, `@Serialization` annotation won't work properly.

**Deserialization:**
<details>
<summary>Straight-forward deserialization</summary>

```kotlin
// add extensions from 'kotlinx' lib to your project:
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.serializer
// add com.akuleshov7:ktoml-core to your project:
import com.akuleshov7.ktoml.deserialize

@Serializable
data class MyClass(/* your fields */)

// to deserialize toml input in a string format (separated by newlines '\n')
// no need to provide serializer() explicitly if you will use extension method from
// <kotlinx.serialization.decodeFromString>
val resultFromString = Toml.decodeFromString<MyClass>(/* string with a toml input */)
val resultFromList = Toml.decodeFromString<MyClass>(serializer(), /* sequence with lines of strings with a toml input */)
```
</details>

<details>
<summary>Partial deserialization</summary>

Partial Deserialization can be useful when you would like to deserialize only **one single** table and you do not want
to reproduce whole object structure in your code.

```kotlin
// If you need to deserialize only some part of the toml - provide the full name of the toml table. 
// The deserializer will work only with this table and it's children.
// For example if you have the following toml, but you want only to decode [c.d.e.f] table: 
// [a]
//   b = 1
// [c.d.e.f]
//   d = "5"

val result = Toml.partiallyDecodeFromString<MyClassOnlyForTable>(serializer(), /* string with a toml input */, "c.d.e.f")
val result = Toml.partiallyDecodeFromString<MyClassOnlyForTable>(serializer(), /* list with toml strings */, "c.d.e.f")
```
</details>

<details>
<summary>Toml File deserialization</summary>

```kotlin
// add com.akuleshov7:ktoml-file to your project
import com.akuleshov7.ktoml.file

val resultFromString = TomlFileReader.decodeFromFile<MyClass>(serializer(), /* file path to toml file */)
val resultFromList = TomlFileReader.partiallyDecodeFromFile<MyClass>(serializer(),  /* file path to toml file */, /* table name */)
```

:heavy_exclamation_mark: `toml-file` is only one of the example for reading the data from source.
For your particular case you can implement your own source provider based on
[okio.Source](https://github.com/square/okio/blob/1d86391ca0ee8e5730fd0bbb6bee94c4a41ad945/okio/src/commonMain/kotlin/okio/Source.kt#L8).
For this purpose we have prepared `toml-source` module and implemented an 
[example](https://github.com/akuleshov7/ktoml/blob/main/ktoml-source/src/jvmMain/kotlin/com/akuleshov7/ktoml/source/JvmStreams.kt) 
with java streams for JVM target.

```kotlin
// add com.akuleshov7:ktoml-source to your project
import com.akuleshov7.ktoml.source

val resultFromString = TomlFileReader.decodeFromSource<MyClass>(serializer(), /* your source */)
val resultFromList = TomlFileReader.partiallyDecodeFromSource<MyClass>(serializer(),  /* your source */, /* table name */)
```
</details>

**Serialization:**
<details>
<summary>Straight-forward serialization</summary>

```kotlin
// add extensions from 'kotlinx' lib to your project:
import kotlinx.serialization.encodeToString
// add com.akuleshov7:ktoml-core to your project:
import com.akuleshov7.ktoml.Toml

@Serializable
data class MyClass(/* your fields */)

val toml = Toml.encodeToString(MyClass(/* ... */))
```
</details>

<details>
<summary>Toml File serialization</summary>

```kotlin
// add com.akuleshov7:ktoml-file to your project
import com.akuleshov7.ktoml.file.TomlFileWriter

TomlFileWriter.encodeToFile<MyClass>(serializer(), /* file path to toml file */)
```
</details>

**Parser to AST:**
<details>
<summary>Simple parser</summary>

```kotlin
import com.akuleshov7.ktoml.parsers.TomlParser
import com.akuleshov7.ktoml.TomlConfig
/* ========= */
var tomlAST = TomlParser(TomlInputConfig()).parseStringsToTomlTree(/* list with toml strings */)
tomlAST = TomlParser(TomlInputConfig()).parseString(/* the string that you want to parse */)
tomlAST.prettyPrint()
```
</details>

### Configuration
Ktoml parsing and deserialization was made configurable to fit all the requirements from users. We have created a
special configuration class that can be passed to the decoder method:

```kotlin
Toml(
    inputConfig = TomlInputConfig(
        // allow/prohibit unknown names during the deserialization, default false
        ignoreUnknownNames = false,
        // allow/prohibit empty values like "a = # comment", default true
        allowEmptyValues = true,
        // allow/prohibit null values like "a = null", default true
        allowNullValues = true,
        // allow/prohibit escaping of single quotes in literal strings, default true
        allowEscapedQuotesInLiteralStrings = true,
        // allow/prohibit processing of empty toml, if false - throws an InternalDecodingException exception, default is true
        allowEmptyToml = true,
    ),
    outputConfig = TomlOutputConfig(
        // indentation symbols for serialization, default 4 spaces
        indentation = Indentation.FOUR_SPACES,
    )
).decodeFromString<MyClass>(
    tomlString
)
```

## How ktoml works: examples
:heavy_exclamation_mark: You can check how below examples work in [decoding ReadMeExampleTest](https://github.com/akuleshov7/ktoml/blob/main/ktoml-core/src/commonTest/kotlin/com/akuleshov7/ktoml/decoders/ReadMeExampleTest.kt) and [encoding ReadMeExampleTest](https://github.com/akuleshov7/ktoml/blob/main/ktoml-core/src/commonTest/kotlin/com/akuleshov7/ktoml/encoders/ReadMeExampleTest.kt).

<details>
<summary>Deserialization</summary>
The following example:

```toml
someBooleanProperty = true
# inline tables in gradle 'libs.versions.toml' notation
gradle-libs-like-property = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }

[table1]
    # null is prohibited by the TOML spec, but allowed in ktoml for nullable types
    # so for 'property1' null value is ok. Use: property1 = null  
    property1 = 100
    property2 = 6

[myMap]
    a = "b"
    c = "d"

[table2]
    someNumber = 5
[table2."akuleshov7.com"]
    name = 'this is a "literal" string'
    # empty lists are also supported
    configurationList = ["a",  "b",  "c"]

    # such redeclaration of table2
    # is prohibited in toml specification;
    # but ktoml is allowing it in non-strict mode: 
    [table2]
        otherNumber = 5.56
        # use single quotes
        charFromString = 'a'
        charFromInteger = 123
```

can be deserialized to `MyClass`:
```kotlin
@Serializable
data class MyClass(
    val someBooleanProperty: Boolean,
    val table1: Table1,
    val table2: Table2,
    @SerialName("gradle-libs-like-property")
    val kotlinJvm: GradlePlugin,
    val myMap: Map<String, String>
)

@Serializable
data class Table1(
    // nullable property, from toml input you can pass "null"/"nil"/"empty" value (no quotes needed) to this field
    val property1: Long?,
    // please note, that according to the specification of toml integer values should be represented with Long,
    // but we allow to use Int/Short/etc. Just be careful with overflow
    val property2: Byte,
    // no need to pass this value in the input as it has the default value and so it is NOT REQUIRED
    val property3: Short = 5
)

@Serializable
data class Table2(
    val someNumber: Long,
    @SerialName("akuleshov7.com")
    val inlineTable: NestedTable,
    val otherNumber: Double,
    // Char in a manner of Java/Kotlin is not supported in TOML, because single quotes are used for literal strings.
    // However, ktoml supports reading Char from both single-char string and from it's integer code
    val charFromString: Char,
    val charFromInteger: Char
)

@Serializable
data class NestedTable(
    val name: String,
    @SerialName("configurationList")
    val overriddenName: List<String?>
)

@Serializable
data class GradlePlugin(val id: String, val version: Version)

@Serializable
data class Version(val ref: String)
```

with the following code:
```kotlin
Toml.decodeFromString<MyClass>(/* your toml string */)
```

Translation of the example above to json-terminology:

```json
{
  "someBooleanProperty": true,
  
  "gradle-libs-like-property": {
    "id": "org.jetbrains.kotlin.jvm",
    "version": {
      "ref": "kotlin"
    }
  },
  
  "table1": {
    "property1": 100,
    "property2": 5
  },
  "table2": {
    "someNumber": 5,
    
    "otherNumber": 5.56,
    "akuleshov7.com": {
      "name": "my name",
      "configurationList": [
        "a",
        "b",
        "c"
      ]
    }
  }
}
``` 
</details>

<details>
<summary>Serialization</summary>
The following example from above:

```toml
someBooleanProperty = true
# inline tables in gradle 'libs.versions.toml' notation
gradle-libs-like-property = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }

[table1]
# null is prohibited by the TOML spec, but allowed in ktoml for nullable types
# so for 'property1' null value is ok. Use: property1 = null. 
# Null can also be prohibited with 'allowNullValues = false'
property1 = 100
property2 = 6

[table2]
    someNumber = 5
    [table2."akuleshov7.com"]
        name = 'this is a "literal" string'
        # empty lists are also supported
        configurationList = ["a",  "b",  "c"]

# such redeclaration of table2
# is prohibited in toml specification;
# but ktoml is allowing it in non-strict mode: 
[table2]
    otherNumber = 5.56
    # use single quotes
    charFromString = 'a'
    charFromInteger = 123
```

can be serialized from `MyClass`:

```kotlin
@Serializable
data class MyClass(
    val someBooleanProperty: Boolean,
    @TomlComments(
        "Comments can be added",
        "More comments can also be added"
    )
    val table1: Table1,
    val table2: Table2,
   @SerialName("gradle-libs-like-property")
   val kotlinJvm: GradlePlugin
)

@Serializable
data class Table1(
    @TomlComments(inline = "At the end of lines too")
    // nullable values, represented as "null" in toml. For more strict behavior,
    // null values can be ignored with the ignoreNullValues config property.
    val property1: Long?,
    // please note, that according to the specification of toml integer values should be represented with Long
    val property2: Long,
    // Default values can be ignored with the ignoreDefaultValues config property.
    val property3: Long = 5
)

@Serializable
data class Table2(
    // Integers can be formatted in hex, binary, etc. Currently only decimal is
    // supported.
    @TomlInteger(IntegerRepresentation.DECIMAL)
    val someNumber: Long,
    @SerialName("akuleshov7.com")
    @TomlInlineTable // Can be on the property
    val inlineTable: InlineTable,
    @TomlComments(
        "Properties always appear before sub-tables, tables aren't redeclared"
    )
    val otherNumber: Double
)

@Serializable
data class InlineTable(
    @TomlLiteral
    val name: String,
    @SerialName("configurationList")
    val overriddenName: List<String?>
)

@Serializable
@TomlInlineTable // ...or the class
data class GradlePlugin(
    val id: String,
    // version is "collapsed": single member inline tables become dotted pairs.
    val version: Version
)

@Serializable
@TomlInlineTable
data class Version(val ref: String)
```

with the following code:

```kotlin
Toml.encodeToString<MyClass>(/* your encoded object */)
```
</details>

## Q&A

<details>
<summary>I want to catch ktoml-specific exceptions in my code, how can I do it?</summary>

Ktoml may generate various exceptions when encountering invalid input. It's important to note that certain strict checks can be enabled or disabled (refer to the `Configuration` section in this readme). We have intentionally exposed only two top-level exceptions, namely `TomlDecodingException` and `TomlEncodingException`, for public use. You can catch these exceptions in your code, as all other exceptions inherit from one of these two and will not be publicly accessible.
</details>

<details>
<summary>What if I do not know the names for keys and tables in my TOML, and therefore cannot specify a strict schema for decoding? Can I still decode it somehow?</summary>

Certainly. In such cases, you can decode all your key-values into a `Map`. However, it's important to be aware that both ktoml and kotlinx will be unable to enforce type control in this scenario. Therefore, you should not expect any "type safety." For instance, even when dealing with a mixture of types like Int, Map, String, etc., such as:

```toml
[a]
    b = 42
    c = "String"
    [a.innerTable]
        d = 5
    [a.otherInnerTable]
        d = "String"
```

You can still decode it using `Toml.decodeFromString<MyClass>(data)` where:

```kotlin
// MyClass(a={b=42, c=String, innerTable={d=5}, otherInnerTable={d=String}})
@Serializable
data class MyClass(
    val a: Map<String, Map<String, String>>
)
```


However, be aware that this may lead to unintended side effects. Our recommendation is to decode only key-values of the **same** type for a more predictable outcome.
</details>
