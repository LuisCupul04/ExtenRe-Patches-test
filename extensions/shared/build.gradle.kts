import java.lang.Boolean.TRUE

plugins {
    id("maven-publish")
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.protobuf)
}

extension {
    name = "extensions/shared.re"
}

android {
    namespace = "com.extenre.extension"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }

    buildTypes {
        release {
            isMinifyEnabled = TRUE
            ndk {
                abiFilters.add("")
            }
        }
    }

    // Habilita la publicación de la variante 'release'
    publishing {
        singleVariant("release") {
            withSourcesJar() // Opcional: incluye las fuentes
        }
    }
}

dependencies {
    api("com.extenre:extenre-patcher:20.0.1.RE")   // api para que sea transitiva

    compileOnly(libs.annotation)
    compileOnly(libs.preference)

    implementation(libs.collections4)
    implementation(libs.gson)
    implementation(libs.lang3)
    implementation(libs.okhttp3)
    implementation(libs.protobuf.javalite)

    implementation("com.github.ynab:J2V8:6.2.1-16kb.2@aar")

    coreLibraryDesugaring(libs.desugar.jdk.libs)
    compileOnly(project(":extensions:shared:stub"))
}

protobuf {
    protoc {
        artifact = libs.protobuf.protoc.get().toString()
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
            }
        }
    }
}

// Configuración de publicación en GitHub Packages
publishing {
    publications {
        create<MavenPublication>("release") {
            afterEvaluate {
                from(components["release"])
            }
            groupId = "com.extenre.extensions"
            artifactId = "shared"
            version = "1.0.0"   // Puedes usar project.version si lo prefieres
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/LuisCupul04/ExtenRe-patches")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}