import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("com.google.devtools.ksp")
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }
}

android {

    val properties = Properties()
    properties.load(FileInputStream(rootProject.file("local.properties")))
    val apiKey: String = properties.getProperty("api_key")
    val secretKey: String = properties.getProperty("secret_key")
    val invokeUrl: String = properties.getProperty("invoke_url")
    val gptKey: String = properties.getProperty("gpt_key")
    val baseUrl: String = properties.getProperty("base_url")

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    sourceSets {
        getByName("main") {
            assets.srcDirs("src/main/assets")
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
        buildConfigField("String", "SECRET_KEY", secretKey)
        buildConfigField("String", "INVOKE_URL", invokeUrl)
        buildConfigField("String", "GPT_KEY", gptKey)
        buildConfigField("String", "BASE_URL", baseUrl)
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

    packaging {
        resources {
            excludes += setOf("META-INF/INDEX.LIST", "META-INF/DEPENDENCIES")
        }
    }
}

dependencies {
    implementation(libs.okhttp.v490)
    implementation(libs.mobile.ffmpeg.full)

    // AndroidX 및 Jetpack
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview) // RecyclerView 추가
    implementation(libs.androidx.lifecycle.viewmodel.ktx) // ViewModel 추가

    // Room 라이브러리
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Retrofit 및 네트워크 라이브러리
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.converter.scalar)
    implementation(libs.okhttp3.okhttp)
    implementation(libs.logging.interceptor)

    // 코루틴 (비동기 작업)
    implementation(libs.kotlinx.coroutines.android)

    // Google Cloud Speech API
    implementation(libs.google.cloud.speech)
    implementation(libs.google.auth.library.oauth2.http)
    implementation(libs.grpc.okhttp)
    implementation(libs.grpc.stub)
    implementation(libs.gax)
    implementation(libs.libraries.bom)

    // Test 및 Android Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
