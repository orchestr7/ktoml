plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    // this hack prevents the following bug: https://github.com/gradle/gradle/issues/9770
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.0")

    implementation("org.cqfn.diktat:diktat-gradle-plugin:1.0.0-rc.3")
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.15.0")
    implementation("io.github.gradle-nexus:publish-plugin:1.1.0")
    implementation("org.ajoberstar.reckon:reckon-gradle:0.13.0")
    implementation("org.ajoberstar.grgit:grgit-core:4.1.0")
}
