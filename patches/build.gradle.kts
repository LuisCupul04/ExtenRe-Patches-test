import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    id("maven-publish")
}

group = "com.extenre"

repositories {
    mavenLocal()
    mavenCentral()
    google()
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/LuisCupul04/Extenre-patcher")
        credentials {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
    maven {
        name = "JitPack"
        url = uri("https://jitpack.io")
    }
}

// Configuración de la extensión 'patches'
patches {
    about {
        name = "ExtenRe Patches"
        author = "LuisCupul04"
        license = "GNU General Public License v3.0"
    }
}

dependencies {
    implementation("com.extenre.extensions:shared:1.0.0")
    implementation("com.extenre:extenre-patcher:20.0.1.RE")
    implementation(libs.gson)
}

tasks {
    jar {
        archiveExtension.set("EXRE")
        exclude("com/extenre/generator")
    }

    register<JavaExec>("generatePatchesFiles") {
        description = "Generate patches files"
        dependsOn(build)
        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set("com.extenre.generator.MainKt")
    }

    named("publish") {
        dependsOn("generatePatchesFiles")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        freeCompilerArgs = listOf("-Xcontext-receivers")
        jvmTarget = JvmTarget.JVM_21
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = group.toString()
            artifactId = "patches"
            version = version ?: "1.0.0"
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

// Opcional: también agregar diagnóstico para JavaCompile en este módulo si tuviera código Java
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Xdiags:verbose"))
    doFirst {
        println("📄 Compilando archivos Java en ${name}:")
        source.files.forEach { println("   - $it") }
    }
}