package com.akuleshov7.ktoml

import com.akuleshov7.ktoml.exceptions.TomlParsingException

fun String.error() = println("[ERROR] $this")

fun String.warn() = println("[WARN] $this")

fun String.info() = println("[INFO] $this")
