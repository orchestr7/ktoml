/**
 * File utils to read files using okio
 */

package com.akuleshov7.ktoml.file

import okio.FileSystem

internal actual fun getOsSpecificFileSystem(): FileSystem = FileSystem.SYSTEM
