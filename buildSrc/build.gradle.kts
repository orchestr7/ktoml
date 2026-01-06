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
    // this hack prevents the following bug: https://github.com/gradle/gradle/issues/9770
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.0")

    implementation("org.cqfn.diktat:diktat-gradle-plugin:1.2.5")
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.23.8")
    implementation("io.github.gradle-nexus:publish-plugin:2.0.0")
    implementation("org.ajoberstar.reckon:reckon-gradle:0.19.2")
    implementation("org.ajoberstar.grgit:grgit-core:5.3.3")
}
