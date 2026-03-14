import java.lang.Boolean.TRUE

plugins {
    id("maven-publish")
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.protobuf)
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
    publishing {
        singleVariant("release")
    }
    
    // Compatibilidad con Java 21
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
}

dependencies {
    api("com.extenre:extenre-patcher:20.0.1.RE")
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

extension {
    name = "extensions/shared.re"
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                artifactId = "shared"
                version = "1.0.0"
            }
        }
        repositories {
            mavenLocal()
        }
    }
}

// Configuración de diagnóstico para el compilador Java
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(
        listOf(
            "-Xlint:all",
            "-Xdiags:verbose",
            "-XDignore.symbol.file" // Ignorar validación estricta de símbolos del JDK
        )
    )
    options.isFork = true
    options.forkOptions.jvmArgs?.add("-XX:+ShowCodeDetailsInExceptionMessages")
    doFirst {
        println("📄 Compilando archivos Java en ${name}:")
        source.files.forEach { println("   - $it") }
    }
}