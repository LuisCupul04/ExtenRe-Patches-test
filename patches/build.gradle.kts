group = "com.extenre"

patches {
    about {
        name = "ExtenRe Patches"
        author = "LuisCupul04"
        license = "GNU General Public License v3.0"
    }
}

dependencies {
    // Used by JsonGenerator.
    implementation(project(":patcher"))
    implementation(project(":extensions:shared"))
    implementation(libs.gson)
}

tasks {
    jar {
        // ⬇️ ESTA ES LA ÚNICA LÍNEA QUE DEBES AÑADIR (o modificar si ya existe)
        archiveExtension.set("EXRE")
        exclude("com/extenre/generator")
    }
    register<JavaExec>("generatePatchesFiles") {
        description = "Generate patches files"

        dependsOn(build)

        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set("com.extenre.generator.MainKt")
    }
    // Used by gradle-semantic-release-plugin.
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
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])  // Publica el JAR principal

            // Opcional: incluir fuentes y javadoc si los generas
            // artifact(tasks["sourcesJar"])   // si tienes una tarea sourcesJar
            // artifact(tasks["javadocJar"])   // si tienes javadocJar
        }
    }
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
}