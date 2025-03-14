plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    // this hack prevents the following bug: https://github.com/gradle/gradle/issues/9770
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.10")

    implementation("org.cqfn.diktat:diktat-gradle-plugin:1.2.5")
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.23.7")
    implementation("io.github.gradle-nexus:publish-plugin:2.0.0")
    implementation("org.ajoberstar.reckon:reckon-gradle:0.18.3")
    implementation("org.ajoberstar.grgit:grgit-core:5.3.0")
}
