package com.akuleshov7.ktoml.parsers

import com.akuleshov7.ktoml.tree.TableType
import com.akuleshov7.ktoml.tree.TomlFile
import com.akuleshov7.ktoml.tree.TomlTablePrimitive
import kotlin.test.Test
import kotlin.test.assertTrue

class TomlNodeTest {

    @Test
    fun findTableChildByName() {
        val fileNode = prepareTree()

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