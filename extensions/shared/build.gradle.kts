import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
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
            isMinifyEnabled = true

            ndk {
                abiFilters.add("armeabi-v7a")
                abiFilters.add("arm64-v8a")
                abiFilters.add("x86")
                abiFilters.add("x86_64")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
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