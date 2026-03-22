plugins {
    alias(libs.plugins.android.application)   // ← CAMBIADO DE LIBRARY A APPLICATION
}

extension {
    name = "extensions/all/connectivity/wifi/spoof/spoof-wifi.re"
}

android {
    namespace = "com.extenre.extension"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.extenre.extension.spoofwifi"   // Necesario para aplicación
        minSdk = 21
        versionCode = 1
        versionName = "1.0"
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
    compileOnly(libs.annotation)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}

// ================== Tarea manual para generar el DEX ==================
val extensionName = extension.name.get()
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

    from(dexOutputDir) {
        include(fileName)
    }
    into(dexOutputDir.parentFile)
}
