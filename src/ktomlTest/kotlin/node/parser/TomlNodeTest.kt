package node.parser

import com.akuleshov7.ktoml.parsers.node.TomlFile
import com.akuleshov7.ktoml.parsers.node.TomlNode
import com.akuleshov7.ktoml.parsers.node.TomlTable
import kotlin.test.Test
import kotlin.test.assertTrue

class TomlNodeTest {

    @Test
    fun findTableChildByName() {
        val fileNode = prepareTree()
        fileNode.prettyPrint()

        assertTrue { fileNode.findTableInAstByName("a.d.e", 3)?.content == "[a.d.e]" }
        assertTrue { fileNode.findTableInAstByName("a.d", 2)?.content == "[a.d]" }
        assertTrue { fileNode.findTableInAstByName("b", 1)?.content == "[b]" }
        assertTrue { fileNode.findTableInAstByName("e", 1) == null }
    }

    /**
     *     a       b
     *    /  \     \
     *   a.c a.d    b.a
     *        \
     *        a.d.e
     */
    fun prepareTree(): TomlFile {
        val fileNode = TomlFile()
        val sectionA = TomlTable("[a]", 0)
        val sectionAC = TomlTable("[a.c]", 1)
        val sectionAD = TomlTable("[a.d]", 2)
        val sectionADE = TomlTable("[a.d.e]", 3)

        val sectionB = TomlTable("[b]", 4)
        val sectionBA = TomlTable("[b.a]", 5)

        fileNode.appendChild(sectionA)
        fileNode.appendChild(sectionB)

        sectionA.appendChild(sectionAC)
        sectionA.appendChild(sectionAD)
        sectionB.appendChild(sectionBA)

        sectionAD.appendChild(sectionADE)

        return fileNode
    }
}