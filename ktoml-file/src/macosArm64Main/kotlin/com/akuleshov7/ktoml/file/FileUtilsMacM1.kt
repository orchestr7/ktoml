/**
 * File utils to read files using okio
 */

@file:Suppress("PACKAGE_NAME_INCORRECT_PATH")

package com.akuleshov7.ktoml.file

import okio.FileSystem

/**
 * Implementation for getting proper file system to read files with okio
 *
 * @return proper FileSystem
 */
internal actual fun getOsSpecificFileSystem(): FileSystem = FileSystem.SYSTEM
