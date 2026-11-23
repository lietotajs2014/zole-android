pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "zole-android"

include(":app")
include(":engine")
include(":ai")
include(":core:ui")
include(":core:data")
include(":feature:menu")
include(":feature:game")
include(":feature:stats")
include(":feature:settings")
