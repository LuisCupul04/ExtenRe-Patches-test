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
    name = "extensions/all/connectivity/wifi/spoof/spoof-wifi.re"
}

android {
    namespace = "com.extenre.extension"
    compileSdk = 35
    defaultConfig { minSdk = 21 }
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
    compileOnly(libs.annotation)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}

// ================== Tarea para generar el DEX ==================
val extensionName = extension.name.get()
val parentPath = extensionName.substringBeforeLast('/')
val fileName = extensionName.substringAfterLast('/')

afterEvaluate {
    val bundleTask = tasks.named("bundleReleaseAar")

    tasks.register<Sync>("syncExtension") {
        dependsOn(bundleTask)

        val aarFileProvider = bundleTask.flatMap { it.outputs.files.single() }
        val extractDir = layout.buildDirectory.dir("tmp/extractAar").get().asFile
        val dexOutputDir = layout.buildDirectory.dir("extenre/$parentPath").get().asFile

        inputs.file(aarFileProvider)
        outputs.dir(dexOutputDir)

        doFirst {
            val aar = aarFileProvider.get().asFile
            logger.lifecycle("Processing AAR: ${aar.absolutePath}")

            extractDir.deleteRecursively()
            extractDir.mkdirs()
            copy {
                from(zipTree(aar))
                into(extractDir)
            }

            val classesJar = extractDir.resolve("classes.jar")
            if (!classesJar.exists()) {
                throw GradleException("classes.jar not found in AAR: $aar")
            }

            dexOutputDir.mkdirs()
            D8Command.builder()
                .addProgramFiles(classesJar.toPath())
                .setOutput(dexOutputDir.toPath(), OutputMode.DexIndexed)
                .build()
                .let(D8::run)

            logger.lifecycle("DEX generated: ${dexOutputDir.listFiles()?.joinToString()}")
        }

        from(dexOutputDir) {
            include("*.dex")
        }
        into(dexOutputDir.parentFile)
        rename { fileName }
    }
}
