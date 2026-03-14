import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.java.TargetJvmVersion
import org.gradle.api.attributes.Attribute

group = "com.extenre"

patches {
    about {
        name = "ExtenRe Patches"
        author = "LuisCupul04"
        license = "GNU General Public License v3.0"
    }
}

configurations.runtimeClasspath {
    attributes {
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.androidJvm)
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
        attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 21)
        attribute(Attribute.of("artifactType", String::class.java), "jar") // ← NUEVO
    }
}

dependencies {
    implementation(project(":extensions:shared")) {
        targetConfiguration = "releaseRuntimeElements"
    }
    implementation("com.extenre:extenre-patcher:20.0.1.RE")
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