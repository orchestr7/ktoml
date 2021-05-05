import com.akuleshov7.buildutils.configureDetekt
import com.akuleshov7.buildutils.configureDiktat
import com.akuleshov7.buildutils.configureVersioning
import com.akuleshov7.buildutils.createDetektTask
import com.akuleshov7.buildutils.createDiktatTask
import com.akuleshov7.buildutils.installGitHooks

plugins {
    kotlin("multiplatform") version Versions.KOTLIN apply false
    kotlin("plugin.serialization") version Versions.KOTLIN apply false
}

configureVersioning()

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
