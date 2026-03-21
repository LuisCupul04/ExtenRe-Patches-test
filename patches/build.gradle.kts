group = "com.extenre"
version = rootProject.properties["version"] as? String ?: "0.0.0"

patches {
    about {
        name = "ExtenRe Patches"
        author = "LuisCupul04"
        license = "GNU General Public License v3.0"
    }
}

dependencies {
    implementation(libs.gson)
    implementation(libs.extenre.patcher)
}

tasks {
    // Bundle de parches (extensión .EXRE)
    jar {
        archiveExtension.set("EXRE")
        exclude("com/extenre/generator")
    }

    // JAR estándar para publicación como biblioteca
    register<Jar>("libraryJar") {
        archiveClassifier.set("")
        from(sourceSets.main.get().output)
        exclude("com/extenre/generator")
    }

    // Genera patches-exre.json y actualiza README
    register<JavaExec>("generatePatchesFiles") {
        description = "Generate patches files"
        dependsOn(build)
        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set("com.extenre.generator.MainKt")
    }

    // Configurar la tarea sourcesJar existente
    named<Jar>("sourcesJar") {
        from(sourceSets.main.get().allSource)
    }

    // Tarea usada por gradle-semantic-release-plugin
    publish {
        dependsOn("generatePatchesFiles")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs = listOf("-Xcontext-receivers")
    }
}

publishing {
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
    publications {
        create<MavenPublication>("patches") {
            // Cambia el artifactId para evitar conflicto con la publicación automática del plugin
            artifactId = "extenre-patches-library"
            artifact(tasks["libraryJar"])
            artifact(tasks["sourcesJar"])
            pom {
                name.set("ExtenRe Patches")
                description.set("Patches for ExtenRe")
                url.set("https://github.com/LuisCupul04/ExtenRe-Patches")
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
                    connection.set("scm:git:git://github.com/LuisCupul04/ExtenRe-Patches.git")
                    developerConnection.set("scm:git:git@github.com:LuisCupul04/ExtenRe-Patches.git")
                    url.set("https://github.com/LuisCupul04/ExtenRe-Patches")
                }
            }
        }
    }
}