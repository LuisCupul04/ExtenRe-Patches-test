import org.gradle.kotlin.dsl.*

plugins {
    java
    kotlin("jvm") version "2.0.21"
    `maven-publish`
}

group = "com.extenre.test"   // o tu grupo real
version = rootProject.properties["version"] as? String ?: "0.0.0"

dependencies {
    implementation(libs.gson)
    implementation(libs.extenre.patcher)
}

tasks.register<JavaExec>("generatePatchesFiles") {
    description = "Generate patches files (JSON and README)"
    dependsOn(tasks.build)
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.extenre.generator.MainKt")
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
            artifactId = "extenre-patches-library"
            artifact(tasks.jar)
            artifact(tasks["sourcesJar"])   // Si defines sourcesJar
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
