pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "taxi-service"

includeBuild("build-logic")

include("gateway-service")
include("user-service")
include("trip-service")
include("payment-service")
include("notification-service")

include("shared-libs:shared-exceptions")
include("shared-libs:shared-dto")
include("shared-libs:shared-security")
