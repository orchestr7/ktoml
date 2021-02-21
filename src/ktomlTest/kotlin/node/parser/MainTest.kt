package node.parser

import com.akuleshov7.parsers.TomlParser
import com.akuleshov7.parsers.node.TomlNode
import okio.ExperimentalFileSystem
import kotlin.test.Test

class MainTest {
    @Test
    @ExperimentalFileSystem
    fun myTest() {
        TomlNode.prettyPrint(TomlParser().readFile("src/ktomlTest/resources/simple_example.toml"))
    }
}