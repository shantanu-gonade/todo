pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
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

rootProject.name = "todo"
include(":app")
include(":core")
include(":core:model")
include(":core:common")
include(":core:database")
include(":core:datastore")
include(":core:data")
include(":core:domain")
include(":core:designsystem")
include(":core:ui")
include(":feature")
include(":feature:today")
include(":feature:history")
