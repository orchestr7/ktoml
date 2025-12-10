import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

repositories {
    // Kotlin 2.2.x artifacts are available on repo1.maven.org but not yet synced to
    // repo.maven.apache.org (which mavenCentral() uses). Once synced, this can be
    // replaced with mavenCentral().
    maven { url = uri("https://repo1.maven.org/maven2/") }
    gradlePluginPortal()
}

dependencies {
    implementation("io.github.gradle-nexus:publish-plugin:2.0.0")
}
