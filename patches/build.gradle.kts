group = "com.extenre"

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
            from(components["java"])
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