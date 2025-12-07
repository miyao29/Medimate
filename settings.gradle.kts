pluginManagement {
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
        //카카오 SDK 저장소 주소
        maven { url = uri("https://devrepo.kakao.com/nexus/content/groups/public/") }
        maven { url =uri("https://jitpack.io") }
    }
}

rootProject.name = "Medimate"
include(":app")
 