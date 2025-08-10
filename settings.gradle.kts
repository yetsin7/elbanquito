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
        maven { url = uri("https://jitpack.io") }
    }
}

dependencyResolutionManagement {
    // CAMBIAR a PREFER_SETTINGS para evitar el error
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
            content {
                includeModule("com.github.PhilJay", "MPAndroidChart")
                includeModule("de.hdodenhof", "circleimageview")
            }
        }
        // Repositorio para iText PDF
        maven { url = uri("https://repo1.maven.org/maven2/") }
    }
}

rootProject.name = "El Banquito"
include(":app")