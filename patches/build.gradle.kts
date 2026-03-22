// patches/build.gradle.kts

import org.gradle.api.tasks.bundling.Jar

group = "com.extenre"
// La versión se lee desde gradle.properties en la raíz del proyecto
version = rootProject.properties["version"] as? String ?: "0.0.0"

plugins {
    // Aplicamos el plugin de Java para compilar el código Kotlin/Java de los parches
    java
    // Opcional: si necesitas generar un JAR "gordo", descomenta shadow
    // id("com.github.johnrengelman.shadow") version "8.1.1"
}

// Lista de submódulos de extensión que producen DEX
val extensionProjects = subprojects.filter { it.path.startsWith(":extensions:") }

// Configuración de la tarea que empaqueta el bundle .EXRE
tasks.jar {
    // Asegura que las extensiones se compilen antes de empaquetar
    dependsOn(extensionProjects.map { it.tasks.named("syncExtension") })

    // Cambia la extensión del archivo de salida a .EXRE
    archiveExtension.set("EXRE")

    // Incluye las clases propias de este módulo (los parches de código)
    from(sourceSets.main.get().output)

    // Incluye los DEX generados por cada extensión
    extensionProjects.forEach { extProject ->
        // Cada extensión coloca sus DEX en build/extenre
        val extDir = extProject.layout.buildDirectory.dir("extenre").get().asFile
        if (extDir.exists()) {
            from(extDir) {
                // Mantiene la estructura de directorios (ej. extensions/all/misc/...)
                into("")
            }
        } else {
            // Si el directorio aún no existe (primera compilación), no falla
            logger.warn("Extension directory not found: ${extDir.absolutePath}. Ensure the extension is built first.")
        }
    }

    // Excluye clases generadoras (no necesarias en el bundle final)
    exclude("com/extenre/generator/**")
}

// Configuración de dependencias
dependencies {
    // Dependencias necesarias para los parches
    implementation(libs.gson)
    implementation(libs.extenre.patcher)

    // Dependencia de las extensiones para tener acceso a sus clases durante la compilación
    extensionProjects.forEach { extProject ->
        implementation(project(extProject.path))
    }
}

// Tarea opcional: genera el archivo patches-RE.json (basado en el código que ya tenías)
tasks.register<JavaExec>("generatePatchesFiles") {
    description = "Generate patches-RE.json and update README"
    dependsOn(tasks.compileJava, tasks.compileKotlin) // o compileKotlin si usas Kotlin
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.extenre.generator.MainKt") // Ajusta si es diferente
}

// Tarea para empaquetar las fuentes (útil para publicación)
tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

// Configuración de publicación Maven a GitHub Packages
publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/LuisCupul04/ExtenRe-patches-test")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: project.findProperty("gpr.user") as? String ?: ""
                password = System.getenv("GITHUB_TOKEN") ?: project.findProperty("gpr.key") as? String ?: ""
            }
        }
    }
    publications {
        create<MavenPublication>("patches") {
            // Publica el bundle .EXRE como artefacto principal
            artifact(tasks.jar)
            // También publica el JAR de fuentes
            artifact(tasks["sourcesJar"])
            pom {
                name.set("ExtenRe Patches")
                description.set("Patches for ExtenRe")
                url.set("https://github.com/LuisCupul04/ExtenRe-patches-test")
                licenses {
                    license {
                        name.set("GNU General Public License v3.0")
                        url.set("https://www.gnu.org/licenses/gpl-3.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("LuisCupul04")
                        name.set("Luis Cupul")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/LuisCupul04/ExtenRe-patches-test.git")
                    developerConnection.set("scm:git:git@github.com:LuisCupul04/ExtenRe-patches-test.git")
                    url.set("https://github.com/LuisCupul04/ExtenRe-patches-test")
                }
            }
        }
    }
}

// Opcional: asegura que la tarea publish dependa de generatePatchesFiles
tasks.named("publish") {
    dependsOn("generatePatchesFiles")
}
