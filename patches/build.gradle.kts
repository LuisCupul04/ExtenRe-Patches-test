// patches/build.gradle.kts
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*

group = "com.extenre.test"
version = rootProject.properties["version"] as? String ?: "0.0.0"

// El plugin com.extenre.patches ya está aplicado en settings.gradle.kts,
// por lo que el bloque "patches" ya está disponible.
patches {
    about {
        name = "ExtenRe Patches"
        author = "LuisCupul04"
        license = "GNU General Public License v3.0"
    }
}

// Dependencias del módulo de parches
dependencies {
    implementation(libs.gson)
    implementation(libs.extenre.patcher)
}

// Lista de submódulos de extensión
val extensionProjects = subprojects.filter { it.path.startsWith(":extensions:") }

// Tarea que genera el bundle .EXRE
tasks.jar {
    archiveExtension.set("EXRE")
    exclude("com/extenre/generator")

    // Asegurar que los DEX de las extensiones estén generados
    dependsOn(extensionProjects.map { it.tasks.named("syncExtension") })

    // Incluir el contenido de build/extenre/ de cada extensión
    from(extensionProjects.map { project ->
        project.layout.buildDirectory.dir("extenre")
    })
}

// Tarea adicional para generar un JAR estándar (biblioteca) sin las extensiones
tasks.register<Jar>("libraryJar") {
    archiveClassifier.set("")
    from(sourceSets.main.get().output)
    exclude("com/extenre/generator")
}

// Tarea que genera el archivo patches-RE.json y actualiza el README
tasks.register<JavaExec>("generatePatchesFiles") {
    description = "Generate patches files (JSON and README)"
    dependsOn(tasks.build)
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.extenre.generator.MainKt")
}

// Configurar la tarea sourcesJar existente (si se usa)
tasks.named<Jar>("sourcesJar") {
    from(sourceSets.main.get().allSource)
}

// Hacer que la tarea 'publish' (usada por semantic-release) dependa de la generación de archivos
tasks.named("publish") {
    dependsOn("generatePatchesFiles")
}

// Configuración de Kotlin (si se usa Kotlin en el módulo)
kotlin {
    compilerOptions {
        freeCompilerArgs = listOf("-Xcontext-receivers")
    }
}

// Publicación a Maven (GitHub Packages)
publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/LuisCupul04/ExtenRe-patches-test")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
    publications {
        create<MavenPublication>("patches") {
            artifactId = "ExtenRe-patches-test-library"
            artifact(tasks["libraryJar"])
            artifact(tasks["sourcesJar"])
            // Si deseas publicar también el bundle .EXRE como artefacto adicional:
            // artifact(tasks.jar) { classifier = "bundle" }
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
