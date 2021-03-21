# ktoml

This tool deserialize:
```text
[table1]
a = 5
b = 6

[table2]
a = 5

    [table2.inlineTable]
        a = "a"
   
```

to 
```kotlin
    @Serializable
    data class MyClass(val table1: Table1, val table2: Table2)

    @Serializable
    data class Table1(val a: Int, val b: Int)

    @Serializable
    data class Table2(val a: Int, val inlineTable: InlineTable)

    @Serializable
    data class InlineTable(val a: String)
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
                 "a": "a"
            }
          }
        }
``` 