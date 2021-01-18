fun main() {
    println("Hello, Kotlin/Native!")


    val path = "readme.md".toPath()
    val entireFileString = fileSystem.read(path) {
        readUtf8()
    }
}


