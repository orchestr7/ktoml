package com.akuleshov7

fun String.error() = println("[ERROR] $this")

fun String.warn() = println("[WARN] $this")

fun String.info() = println("[INFO] $this")
