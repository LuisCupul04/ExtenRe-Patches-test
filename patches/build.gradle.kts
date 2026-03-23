import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions

plugins {
    java
    application
    kotlin("jvm") version "2.0.21"   // o la versión que uses en libs.versions.toml
    `maven-publish`
}

group = "com.extenre.test"
version = rootProject.properties["version"] as? String ?: "0.0.0"

dependencies {
    implementation(libs.gson)
    implementation(libs.extenre.patcher)
}

val extensionProjects = subprojects.filter { it.path.startsWith(":extensions:") }

tasks.jar {
    archiveExtension.set("EXRE")
    exclude("com/extenre/generator")
    dependsOn(extensionProjects.map { it.tasks.named("syncExtension") })
    from(extensionProjects.map { project ->
        project.layout.buildDirectory.dir("extenre")
    })
}

tasks.register<Jar>("libraryJar") {
    archiveClassifier.set("")
    from(sourceSets.main.get().output)
    exclude("com/extenre/generator")
}

tasks.register<JavaExec>("generatePatchesFiles") {
    description = "Generate patches files (JSON and README)"
    dependsOn(tasks.build)
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.extenre.generator.MainKt")
}

tasks.named<Jar>("sourcesJar") {
    from(sourceSets.main.get().allSource)
}

tasks.named("publish") {
    dependsOn("generatePatchesFiles")
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
