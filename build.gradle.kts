import com.akuleshov7.buildutils.*

plugins {
    kotlin("multiplatform") apply false
    kotlin("plugin.serialization") version Versions.KOTLIN apply false
}

configureVersioning()

allprojects {
    repositories {
        mavenCentral()
    }
    configureDiktat()
    configureDetekt()
}
createDiktatTask()
createDetektTask()
installGitHooks()

configurePublishing()
