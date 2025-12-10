pluginManagement {
    repositories {
        // Kotlin 2.2.x artifacts are available on repo1.maven.org but not yet synced to
        // repo.maven.apache.org (which mavenCentral() uses). Once synced, this can be
        // replaced with mavenCentral().
        maven { url = uri("https://repo1.maven.org/maven2/") }
        gradlePluginPortal()
    }
}
