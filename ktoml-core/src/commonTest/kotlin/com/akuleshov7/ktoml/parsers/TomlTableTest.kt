package com.akuleshov7.ktoml.parsers

import com.akuleshov7.ktoml.tree.TableType
import com.akuleshov7.ktoml.tree.TomlFile
import com.akuleshov7.ktoml.tree.TomlTablePrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class TomlTableTest {
    @Test
    fun createTomlTable() {
        val table = TomlTablePrimitive("[a.b.c.d.e]", 0)
        assertEquals(table.tablesList, listOf("a", "a.b", "a.b.c", "a.b.c.d", "a.b.c.d.e"))
        assertEquals(table.content, "[a.b.c.d.e]")
        assertEquals(table.fullTableName, "a.b.c.d.e")
    }

    @Test
    fun createSimpleTomlTable() {
        val table = TomlTablePrimitive("[a]", 0)
        assertEquals(table.fullTableName, "a")
    }

    @Test
    fun insertFirstLevelTableToTreeTest() {
        val fileNode = prepareTree()
        val tableA = TomlTablePrimitive("[a]", 0)
        val tableB = TomlTablePrimitive("[b]", 0)
        val tableC = TomlTablePrimitive("[c]", 0)
        val tableD = TomlTablePrimitive("[d]", 0)

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
        assertEquals( "a", fileNode.findTableInAstByName("a", 1)?.fullTableName,)
        assertEquals( "b", fileNode.findTableInAstByName("b", 1)?.fullTableName,)
        assertEquals( "c", fileNode.findTableInAstByName("c", 1)?.fullTableName,)
        assertEquals( "d", fileNode.findTableInAstByName("d", 1)?.fullTableName,)
    }

    @Test
    fun insertComplexLevelTableToTreeTest() {
        val fileNode = prepareTree()
        val tableA = TomlTablePrimitive("[a.c]", 0)
        val tableB = TomlTablePrimitive("[b.a.a.a]", 0)
        val tableC = TomlTablePrimitive("[c.a.b]", 0)
        val tableD = TomlTablePrimitive("[d.e.f]", 0)

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

        assertEquals( "a.c", fileNode.findTableInAstByName("a.c", 2)?.fullTableName,)
        assertEquals( "b.a.a.a", fileNode.findTableInAstByName("b.a.a.a", 4)?.fullTableName,)
        assertEquals( "c.a.b", fileNode.findTableInAstByName("c.a.b", 3)?.fullTableName,)
        assertEquals( "d.e.f", fileNode.findTableInAstByName("d.e.f", 3)?.fullTableName,)

        // checking that table [b.a.a.a] is a node in a tree that does not have children and it's grandparent is [b]
        val baaaNode = fileNode.findTableInAstByName("b.a.a.a", 4)!!
        val ba = fileNode.findTableInAstByName("b", 1)!!
        assertEquals(emptyList(), baaaNode.children)
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
        val sectionA = TomlTablePrimitive("[a]", 0)
        val sectionAC = TomlTablePrimitive("[a.c]", 1)
        val sectionAD = TomlTablePrimitive("[a.d]", 2)
        val sectionADE = TomlTablePrimitive("[a.d.e]", 3)

        val sectionB = TomlTablePrimitive("[b]", 4)
        val sectionBA = TomlTablePrimitive("[b.a]", 5)

        fileNode.appendChild(sectionA)
        fileNode.appendChild(sectionB)

        sectionA.appendChild(sectionAC)
        sectionA.appendChild(sectionAD)
        sectionB.appendChild(sectionBA)

        sectionAD.appendChild(sectionADE)

        return fileNode
    }
}
