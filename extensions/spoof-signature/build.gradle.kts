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
}

extension {
    name = "extensions/all/misc/signature/spoof-signature.re"
}

android {
    namespace = "com.extenre.extension"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
    }

    buildTypes {
        release {
            isMinifyEnabled = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.hiddenapi)
}

// ================== Tarea manual para generar el DEX ==================
val extensionName = extension.name.get()
val parentPath = extensionName.substringBeforeLast('/')
val fileName = extensionName.substringAfterLast('/')

afterEvaluate {
    tasks.register<Sync>("syncExtension") {
        dependsOn("bundleReleaseAar")

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
