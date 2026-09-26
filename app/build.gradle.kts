plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.modelviewer"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.modelviewer"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    androidResources {
        noCompress += "glb"
    }


    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")

    val composeBom = platform("androidx.compose:compose-bom:2024.09.02")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.material:material-icons-extended")
    // SceneView: Compose wrapper around Filament. Pin an exact version compatible with
    // minSdk 24 / Kotlin 1.9.x — check io.github.sceneview releases before syncing;
    // API surface (SceneView composable args, ModelNode, CameraManipulator) has moved
    // between minor versions, so treat the calls in engine/ and ui/ as "adjust to match
    // your installed version" rather than guaranteed-exact.
    implementation("io.github.sceneview:sceneview:2.2.1")

}