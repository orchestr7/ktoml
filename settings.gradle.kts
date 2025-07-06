plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("1.0.0")
}

rootProject.name = "ktoml"

includeBuild("gradle/plugins")
include("ktoml-core")
include("ktoml-file")
include("ktoml-source")
