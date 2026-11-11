rootProject.name = "Gestiora"

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":androidApp")
include(":shared:core:common")
include(":shared:core:viewmodels")
include(":shared:core:ui")
include(":shared:core:di")
include(":shared:domain:models")
include(":shared:domain:repository")
include(":shared:domain:usecases")
include(":shared:data:models")
include(":shared:data:datasources")
include(":shared:data:datasources-core")
include(":shared:data:repository")
include(":shared:presentation:viewmodels")
include(":shared:presentation:ui")
