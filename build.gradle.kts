import com.akuleshov7.buildutils.*

plugins {
    kotlin("multiplatform") version Versions.KOTLIN apply false
    kotlin("plugin.serialization") version Versions.KOTLIN apply false
}

configureVersioning()
group = "com.akuleshov7"
description = "TOML serialization library for Kotlin language (including Kotlin Native, js, jvm)"

allprojects {
    repositories {
        mavenCentral()
        maven(url = "https://kotlin.bintray.com/kotlinx/")
    }
    configureDiktat()
    configureDetekt()
}
createDiktatTask()
createDetektTask()
installGitHooks()

configurePublishing()
