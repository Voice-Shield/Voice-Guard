import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)

}

buildscript {
    repositories {
        google()
        mavenCentral()

    }
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.9.0")
    implementation("com.arthenica:mobile-ffmpeg-full:4.4.LTS")
}

android {

    val properties = Properties()
    properties.load(FileInputStream(rootProject.file("local.properties")))
    val apiKey: String = properties.getProperty("api_key")

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    sourceSets {
        getByName("main") {
            assets.srcDirs("src/main/assets")
        }
    }
    packagingOptions {
        resources {
            pickFirsts += setOf("META-INF/INDEX.LIST", "META-INF/DEPENDENCIES")
        }
    }


    namespace = "com.example.fishingcatch0403"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.fishingcatch0403"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "API_KEY", apiKey)
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }


    dependencies {
        val nav_version = "2.7.7"
        val lifecycle_version = "2.8.4"

        implementation("androidx.navigation:navigation-fragment-ktx:$nav_version")
        implementation("androidx.navigation:navigation-ui-ktx:$nav_version")
        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.appcompat)
        implementation(libs.material)
        implementation(libs.androidx.activity)
        implementation(libs.androidx.constraintlayout)
        testImplementation(libs.junit)
        testImplementation(libs.coroutine)
        androidTestImplementation(libs.androidx.junit)
        androidTestImplementation(libs.androidx.espresso.core)

        implementation("com.google.cloud:google-cloud-speech:4.2.0")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
        implementation("com.google.auth:google-auth-library-oauth2-http:1.14.0")
        implementation("io.grpc:grpc-okhttp:1.51.0")
        implementation("io.grpc:grpc-stub:1.51.0")
        implementation("com.google.api:gax:2.22.0")
        implementation("com.google.cloud:libraries-bom:26.2.0")

        // ViewModel
        implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycle_version")
    }

}
