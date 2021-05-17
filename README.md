## ktoml 

![Releases](https://img.shields.io/github/v/release/akuleshov7/ktoml)
![Maven Central](https://img.shields.io/maven-central/v/com.akuleshov7/ktoml-core)
![License](https://img.shields.io/github/license/akuleshov7/ktoml)

![Build and test](https://github.com/akuleshov7/ktoml/actions/workflows/build_and_test.yml/badge.svg?branch=main)

![Lines of code](https://img.shields.io/tokei/lines/github/akuleshov7/ktoml)
![Hits-of-Code](https://hitsofcode.com/github/akuleshov7/ktoml?branch=main)
![GitHub repo size](https://img.shields.io/github/repo-size/akuleshov7/ktoml)

![codebeat badge](https://codebeat.co/badges/0518ea49-71ed-4bfd-8dd3-62da7034eebd)
![maintainability](https://api.codeclimate.com/v1/badges/c75d2d6b0d44cea7aefe/maintainability)

Absolutely Native and multiplatform Kotlin serialization library for serialization/deserialization of [toml](https://toml.io/en/) format. Uses `kotlinx.serialization`; uses only native serialization provided by Kotlin, no Java code.

## Supported platforms

All the code is written in Kotlin common module. That means that it can be built for each and every native platform. But to reduce the scope, ktoml supports only the following platforms (if needed - other platfroms could be added later):
 - jvm
 - mingwx64
 - linuxx64
 - macosx64


## Dependency
To import `ktoml` library you need to add following dependencies to your code: 

<details>
<summary>Maven</summary>        "Incorrect format of Key-Value pair. It has empty <value>: $content"


```pom
<dependency>
  <groupId>com.akuleshov7</groupId>
  <artifactId>ktoml-core</artifactId>
  <version>0.2.2</version>
</dependency>
```
</details>

<details>
<summary>Gradle Groovy</summary>

```groovy
implementation 'com.akuleshov7:ktoml-core:0.2.2'
```
</details>

<details>
<summary>Gradle Kotlin</summary>

```kotlin
implementation("com.akuleshov7:ktoml-core:0.2.2")
```
</details>

## How to use

**Deserialization:**
```kotlin
import com.akuleshov7.ktoml.deserialize
/* ========= */
@Serializable
data class MyClass(/* your fields */)

// to deserialize toml input in a string format (separated by newlines '\n')
val result = deserialize<MyClass>(/* string with a toml input */)

// to deserialize toml input from file
val result = deserializeFile<MyClass>(/* string with a path to a toml file */)
```

**Parser to AST:**
```kotlin
import com.akuleshov7.ktoml.parsers.TomlParser
/* ========= */
TomlParser(/* path to your file */).readAndParseFile()
TomlParser(/* the string that you will try to parse */).parseString()
```

## How it works with examples

This tool natively deserializes toml expressions using native Kotlin compiler plug-in and [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/serialization-guide.md).

from toml test:
```text
e = 777

[table1]
a = 5
b = 6

[table2]
a = 5

    [table2.inlineTable]
        a = "a"
        b = A
```

to `MyClass`
```kotlin
    enum class TestEnum {
        A, B, C
    }

    @Serializable
    data class MyClass(val e: Int, val table1: Table1, val table2: Table2)

    @Serializable
    data class Table1(val a: Int, val b: Int)

    @Serializable
    data class Table2(val a: Int, val inlineTable: InlineTable)

    @Serializable
    data class InlineTable(val a: String, val b: TestEnum)
```

Or in json-terminology:
```json
        {
          "table1": {
            "a": 5,
            "b": 5
            },
          "table2": {
             "a": 5,
             "inlineTable": {
                 "a": "a",
                  "b": A
            }
          }
        }
``` 
