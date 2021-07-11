## <img src="/ktoml.png" width="300px"/>

![Releases](https://img.shields.io/github/v/release/akuleshov7/ktoml)
![Maven Central](https://img.shields.io/maven-central/v/com.akuleshov7/ktoml-core)
![License](https://img.shields.io/github/license/akuleshov7/ktoml)
![Build and test](https://github.com/akuleshov7/ktoml/actions/workflows/build_and_test.yml/badge.svg?branch=main)
![Lines of code](https://img.shields.io/tokei/lines/github/akuleshov7/ktoml)
![Hits-of-Code](https://hitsofcode.com/github/akuleshov7/ktoml?branch=main)
![GitHub repo size](https://img.shields.io/github/repo-size/akuleshov7/ktoml)
![codebeat badge](https://codebeat.co/badges/0518ea49-71ed-4bfd-8dd3-62da7034eebd)
![maintainability](https://api.codeclimate.com/v1/badges/c75d2d6b0d44cea7aefe/maintainability)
![Run deteKT](https://github.com/akuleshov7/ktoml/actions/workflows/detekt.yml/badge.svg)
![Run diKTat](https://github.com/akuleshov7/ktoml/actions/workflows/diktat.yml/badge.svg)

Fully Native and Multiplatform Kotlin serialization library for serialization/deserialization of [toml](https://toml.io/en/) format.
Uses native [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization), provided by Kotlin. This library contains no Java code and no Java dependencies.
We believe that TOML is actually the most readable and user-friendly **configuration file** format.
So we decided to support this format for the `kotlinx` serialization library.  

## Contribution
As this young and big project [is needed](https://github.com/Kotlin/kotlinx.serialization/issues/1092) by the Kotlin community, we need your help.
We will be glad if you will test `ktoml` or contribute to this project. 
In case you don't have much time for this - at least spend 5 seconds to give us a star to attract other contributors!
**Thanks!** :pray:

## Supported platforms
All the code is written in Kotlin **common** module. This means that it can be built for each and every Kotlin native platform.
However, to reduce the scope, ktoml now supports only the following platforms:
 - jvm
 - mingwx64
 - linuxx64
 - macosx64

Other platforms could be added later on the demand or easily built by users on their machines.

## Current limitations
**General** \
We are still developing and testing this library, so it has several limitations: \
:white_check_mark: deserialization (with some parsing limitations) \
:x: serialization (not implemented [yet](https://github.com/akuleshov7/ktoml/issues/11), less important for TOML config-files)

**Parsing** \
:white_check_mark: Table sections (single and dotted) \
:white_check_mark: Key-value pairs (single and dotted) \
:white_check_mark: Integer type \
:white_check_mark: Float type \
:white_check_mark: String type \
:white_check_mark: Float type \
:white_check_mark: Boolean type \
:white_check_mark: Simple Arrays \
:white_check_mark: Comments \
:x: Arrays: nested; multiline; of Different Types \
:x: Literal Strings \
:x: Multiline Strings \
:x: Inline Tables \
:x: Array of Tables \
:x: Offset Date-Time, Local: Date-Time; Date; Time 

## Dependency
The library is hosted on the [Maven Central](https://search.maven.org/artifact/com.akuleshov7/ktoml-core).
To import `ktoml` library you need to add following dependencies to your code: 
<details>
<summary>Maven</summary>

```pom
<dependency>
  <groupId>com.akuleshov7</groupId>
  <artifactId>ktoml-core</artifactId>
  <version>0.2.6</version>
</dependency>
```
</details>

<details>
<summary>Gradle Groovy</summary>

```groovy
implementation 'com.akuleshov7:ktoml-core:0.2.6'
```
</details>

<details>
<summary>Gradle Kotlin</summary>

```kotlin
implementation("com.akuleshov7:ktoml-core:0.2.6")
```
</details>


## How to use
:heavy_exclamation_mark: as TOML is a foremost language for config files, we have also supported the deserialization from file.
However, we are using [okio](https://github.com/square/okio) to read the file, so it will be added as a dependency to your project.

:bangbang: there are two ways of calling TOML serialization API:
- Mutable: you need to create a serialization instance once and call serialization methods on it later (1);
- Immutable: in case you need to read a few TOML configurations you can use String extension API methods (2); 

**Deserialization:**
```kotlin
import com.akuleshov7.ktoml.deserialize
/* ========= */
@Serializable
data class MyClass(/* your fields */)

// to deserialize toml input in a string format (separated by newlines '\n') (2)
val result = /* string with a toml input */.deserializeToml<MyClass>()

// to deserialize toml input from file (2)
val result = /* string with path to a toml file */.deserializeTomlFile<MyClass>()

// in case you need optimized code, you can create object of serialization class only once (1)
// it is a useful optimization for loops 
val myTomlSerializer = Toml()
val result1: MyClass1 = myTomlSerializer.decodeFromString(serializer(), /* string with a toml input */)
val result2: MyClass2 = myTomlSerializer.decodeFromString(serializer(), /* string with a toml input */)
```

**Partial Deserialization:** \
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

val result = /* string with a toml input */.deserializeToml<MyClassOnlyForTable>("c.d.e.f")
val result = /* string with path to a toml file */.deserializeTomlFile<MyClassOnlyForTable>("c.d.e.f")
```

**Parser to AST:**
```kotlin
import com.akuleshov7.ktoml.parsers.TomlParser
/* ========= */
TomlParser(KtomlConf()).readAndParseFile(/* path to your file */)
TomlParser(KtomlConf()).parseString(/* the string that you will try to parse */)
```

## How ktoml works: examples

This tool natively deserializes toml expressions using native Kotlin compiler plug-in and [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/serialization-guide.md).

The following example:
```toml
someBooleanProperty = true

[table1]
property1 = 5
property2 = 6
 
[table2]
someNumber = 5
   [table2."akuleshov7.com"]
       name = "my name"
       configurationList = ["a",  "b",  "c"]

# such redeclaration of table2
# is prohibited in toml specification;
# but ktoml is allowing it in non-strict mode: 
[table2]       
otherNumber = 5.56
```

can be deserialized to `MyClass`:
```kotlin
    @Serializable
    data class MyClass(
        val someBooleanProperty: Boolean,
        val table1: Table1,
        val table2: Table2
    )

    @Serializable
    data class Table1(val property1: Int, val property2: Int)

    @Serializable
    data class Table2(
        val someNumber: Int,
        @SerialName("akuleshov7.com")
        val inlineTable: InlineTable,
        val otherNumber: Double
    )

    @Serializable
    data class InlineTable(
        val name: String,
        @SerialName("configurationList")
        val overriddenName: List<String>
    )
```

with the following code:
```kotlin
stringWhereTomlIsStored.deserialize<MyClass>()
```

Translation of the example above to json-terminology:
```json
{
  "someBooleanProperty": true,
  "table1": {
    "property1": 5,
    "property2": 5
    },
  "table2": {
     "someNumber": 5,
     "akuleshov7.com": {
         "name": "my name",
         "configurationList": ["a",  "b",  "c"],
     "otherNumber": 5.56
    }
  }
}
``` 

You can check how this example works in [ReadMeExampleTest](ktoml-core/src/commonTest/kotlin/decoder/ReadMeExampleTest.kt).

### Configuration
Ktoml parsing and deserialization was made configurable to fit all the requirements from users.
We have created a special configuration class that can be passed to the decoder method:
```kotlin
stringWhereTomlIsStored.deserialize<MyClass>(
    ktomlConf = KtomlConf(
        // allow/prohibit unknown names during the deserialization
        ignoreUnknownNames= false, 
        // allow/prohibit empty values like "a = # comment"
        emptyValuesAllowed = true
    )   
)
```
