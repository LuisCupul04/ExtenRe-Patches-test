pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        mavenLocal() // ¡Importante!
        maven { setUrl("https://jitpack.io") }
        // tus repositorios con credenciales
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/LuisCupul04/ExtenRe-patcher")
            credentials {
                username = providers.gradleProperty("gpr.user").getOrElse(System.getenv("GITHUB_ACTOR"))
                password = providers.gradleProperty("gpr.key").getOrElse(System.getenv("GITHUB_TOKEN"))
            }
        }
        maven {
            name = "GitHubPackages2"
            url = uri("https://maven.pkg.github.com/LuisCupul04/smali-RE")
            credentials {
                username = providers.gradleProperty("gpr.user").getOrElse(System.getenv("GITHUB_ACTOR"))
                password = providers.gradleProperty("gpr.key").getOrElse(System.getenv("GITHUB_TOKEN"))
            }
        }
    }
}

plugins {
    id("com.extenre.patches") version "1.0.3.dev-RE"
}

rootProject.name = "extenre-patches"

include(":patches")
include(":extensions:shared:stub")
include(":extensions:shared")
include(":extensions:spoof-signature")
include(":extensions:spoof-wifi")