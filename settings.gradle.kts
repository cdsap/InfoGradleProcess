pluginManagement {
    repositories {
        exclusiveContent {
            forRepository { gradlePluginPortal() }
            filter {
                includeGroupByRegex("com\\.gradle.*")
                includeGroupByRegex("org\\.gradle.*")
            }
        }
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        exclusiveContent {
            forRepository { gradlePluginPortal() }
            filter {
                includeGroupByRegex("com\\.gradle.*")
                includeGroupByRegex("org\\.gradle.*")
            }
        }
        mavenCentral()
    }
}
rootProject.name = "InfoGradleProcess"
