plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "com.eddymy1304.scanner.sample"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.eddymy1304.scanner.sample"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(projects.sample.composeApp)
    implementation(libs.androidx.activity.compose)
}
