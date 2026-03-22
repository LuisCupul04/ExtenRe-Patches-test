import org.jetbrains.kotlin.gradle.dsl.JvmTarget

buildscript {
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    id("com.android.application")
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.protobuf)
}

val extensionName = "extensions/shared.re"

android {
    namespace = "com.extenre.extension"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.extenre.extension.shared"
        minSdk = 24
        versionCode = 1
        versionName = "1.0"
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

val parentPath = extensionName.substringBeforeLast('/')
val fileName = extensionName.substringAfterLast('/')

tasks.register<Sync>("syncExtension") {
    dependsOn("assembleRelease")

    val apkFile = layout.buildDirectory.file("outputs/apk/release/${project.name}-release.apk").get().asFile
    val extractDir = layout.buildDirectory.dir("tmp/extractApk").get().asFile
    val dexOutputDir = layout.buildDirectory.dir("extenre/$parentPath").get().asFile

    doFirst {
        extractDir.deleteRecursively()
        extractDir.mkdirs()
        copy {
            from(zipTree(apkFile))
            into(extractDir)
        }

        val classesDex = extractDir.resolve("classes.dex")
        if (!classesDex.exists()) {
            throw GradleException("classes.dex not found in APK: $apkFile")
        }

        dexOutputDir.mkdirs()
        copy {
            from(classesDex)
            into(dexOutputDir)
            rename { fileName }
        }
    }

    from(dexOutputDir) { include(fileName) }
    into(dexOutputDir.parentFile)
}
