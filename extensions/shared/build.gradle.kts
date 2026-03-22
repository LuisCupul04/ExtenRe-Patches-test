import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.android.tools.r8.D8
import com.android.tools.r8.D8Command
import com.android.tools.r8.OutputMode

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools:r8:8.3.36")
    }
}

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

// ================== Tarea manual para generar el DEX ==================
val extensionName = extension.name.get()
val parentPath = extensionName.substringBeforeLast('/')
val fileName = extensionName.substringAfterLast('/')

afterEvaluate {
    tasks.register<Sync>("syncExtension") {
        dependsOn("bundleReleaseAar")   // ← Usar la tarea que genera el AAR

        val aarFile = layout.buildDirectory.file("outputs/aar/${project.name}-release.aar").get().asFile
        val extractDir = layout.buildDirectory.dir("tmp/extractAar").get().asFile
        val dexOutputDir = layout.buildDirectory.dir("extenre/$parentPath").get().asFile

        doFirst {
            extractDir.deleteRecursively()
            extractDir.mkdirs()
            copy {
                from(zipTree(aarFile))
                into(extractDir)
            }

            val classesJar = extractDir.resolve("classes.jar")
            if (!classesJar.exists()) {
                throw GradleException("classes.jar not found in AAR: $aarFile")
            }

            dexOutputDir.mkdirs()
            D8Command.builder()
                .addProgramFiles(classesJar.toPath())
                .setOutput(dexOutputDir.toPath(), OutputMode.DexIndexed)
                .build()
                .let(D8::run)
        }

        from(dexOutputDir) {
            include("*.dex")
        }
        into(dexOutputDir.parentFile)
        rename { fileName }
    }
}
