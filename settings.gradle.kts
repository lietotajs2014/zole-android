pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
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
