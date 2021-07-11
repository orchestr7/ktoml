Fully Native and Multiplatform Kotlin serialization library for serialization/deserialization of [toml](https://toml.io/en/) format.
Uses native [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization), provided by Kotlin. This library contains no Java code and no Java dependencies.
We believe that TOML is actually the most readable and user-friendly *configuration file* format.
So we decided to support this format for the kotlinx serialization library.  

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
