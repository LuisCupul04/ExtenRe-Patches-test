plugins {
    alias(libs.plugins.android.library)   // ← Agregado
}

extension {
    name = "extensions/all/misc/signature/spoof-signature.re"
}

android {
    namespace = "com.extenre.extension"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
    }

    buildTypes {
        release {
            isMinifyEnabled = true   // ← Cambiado de TRUE a true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.hiddenapi)
}