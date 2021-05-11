import com.akuleshov7.buildutils.*

plugins {
    kotlin("multiplatform") version Versions.KOTLIN apply false
    kotlin("plugin.serialization") version Versions.KOTLIN apply false
}


project.group = "com.akuleshov7"
project.description = "TOML serialization library for Kotlin language (including Kotlin Native, js, jvm)"
project.version = "0.2.0"


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
