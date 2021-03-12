package node.parser

import com.akuleshov7.ktoml.parsers.node.TomlFile
import com.akuleshov7.ktoml.parsers.node.TomlNode
import com.akuleshov7.ktoml.parsers.node.TomlTable
import kotlin.test.Test
import kotlin.test.assertEquals

class TomlTableTest {
    @Test
    fun createTomlTable() {
        val table = TomlTable("[a.b.c.d.e]", 0)
        assertEquals(table.tablesList, listOf("a", "a.b", "a.b.c", "a.b.c.d", "a.b.c.d.e"))
        assertEquals(table.content, "[a.b.c.d.e]")
        assertEquals(table.level, 4)
        assertEquals(table.tableName, "a.b.c.d.e")
    }

    @Test
    fun createSimpleTomlTable() {
        val table = TomlTable("[a]", 0)
        assertEquals(table.level, 0)
    }

    @Test
    fun insertFirstLevelTableToTreeTest() {
        val fileNode = prepareTree()
        val tableA = TomlTable("[a]", 0)
        val tableB = TomlTable("[b]", 0)
        val tableC = TomlTable("[c]", 0)
        val tableD = TomlTable("[d]", 0)

        fileNode.insertTableToTree(tableA)
        fileNode.insertTableToTree(tableB)
        fileNode.insertTableToTree(tableC)
        fileNode.insertTableToTree(tableD)

        /**
         *     a       b                a       b     c   d
         *    /  \     \               /  \     \     \   \
         *   a.c a.d    b.a    =>    a.c a.d    b.a
         *        \                        \
         *        a.d.e                   a.d.e
         */
        assertEquals( "a", fileNode.findTableInAstByName("a", 1)?.tableName,)
        assertEquals( "b", fileNode.findTableInAstByName("b", 1)?.tableName,)
        assertEquals( "c", fileNode.findTableInAstByName("c", 1)?.tableName,)
        assertEquals( "d", fileNode.findTableInAstByName("d", 1)?.tableName,)
    }

    @Test
    fun insertComplexLevelTableToTreeTest() {
        val fileNode = prepareTree()
        val tableA = TomlTable("[a.c]", 0)
        val tableB = TomlTable("[b.a.a.a]", 0)
        val tableC = TomlTable("[c.a.b]", 0)
        val tableD = TomlTable("[d.e.f]", 0)

        fileNode.insertTableToTree(tableA)
        fileNode.insertTableToTree(tableB)
        fileNode.insertTableToTree(tableC)
        fileNode.insertTableToTree(tableD)

        /**
         *     a       b                a       b     c       d
         *    /  \     \               /  \     \     \       \
         *   a.c a.d    b.a    =>    a.c a.d    b.a   c.a     d.e
         *        \                        \     \      \      \
         *        a.d.e                   a.d.e  b.a.a  c.a.b  d.e.f
         *                                         \
         *                                       b.a.a.a
         */

        assertEquals( "a.c", fileNode.findTableInAstByName("a.c", 2)?.tableName,)
        assertEquals( "b.a.a.a", fileNode.findTableInAstByName("b.a.a.a", 4)?.tableName,)
        assertEquals( "c.a.b", fileNode.findTableInAstByName("c.a.b", 3)?.tableName,)
        assertEquals( "d.e.f", fileNode.findTableInAstByName("d.e.f", 3)?.tableName,)

        // checking that table [b.a.a.a] is a node in a tree that does not have children and it's grandparent is [b]
        val baaaNode = fileNode.findTableInAstByName("b.a.a.a", 4)!!
        val ba = fileNode.findTableInAstByName("b", 1)!!
        assertEquals(emptySet(), baaaNode.children)
        assertEquals(ba, baaaNode.parent?.parent?.parent)
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
