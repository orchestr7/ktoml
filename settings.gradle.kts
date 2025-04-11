plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.10.0")
}

rootProject.name = "ktoml"

includeBuild("gradle/plugins")
include("ktoml-core")
include("ktoml-file")
include("ktoml-source")
