import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

// --- 1. local.properties 파일 읽기 (추가) ---
// (plugins 블록 밖, android 블록 위에 추가해야 합니다)
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}
// ------------------------------------------

android {
    namespace = "com.example.medimate"
    compileSdk = 36

    // --- 2. buildConfig 기능 활성화 (추가) ---
    // (defaultConfig 블록 위에 추가)
    buildFeatures {
        buildConfig = true
    }
    // ----------------------------------------

    defaultConfig {
        applicationId = "com.example.medimate"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // --- 3. API 키를 BuildConfig 변수로 주입 (추가) ---
        // local.properties에서 "OPENAI_API_KEY" 값을 읽어옴
        val apiKey = localProperties.getProperty("OPENAI_API_KEY") ?: ""
        // BuildConfig.OPENAI_API_KEY 변수 생성
        buildConfigField("String", "OPENAI_API_KEY", "\"$apiKey\"")
        // ------------------------------------------------
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    // 기본 라이브러리
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // --- 기능 라이브러리 (정리됨) ---

    // 1. ML Kit (Korean OCR) - 최신 버전(16.0.1)만 남김
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.google.mlkit:text-recognition-korean:16.0.1")

    // 2. Retrofit & OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0") // 최신 버전(4.12.0)만 남김

    // (기타: XML 변환기 - 원래 있었으므로 유지)
    implementation("org.simpleframework:simple-xml:2.7.1")
    implementation("com.squareup.retrofit2:converter-simplexml:2.9.0")

    // --- 테스트 라이브러리 ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // ↓↓↓ ViewModel 라이브러리
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.8.0")
    implementation("androidx.lifecycle:lifecycle-livedata:2.8.0")

    //recommendation
    implementation("org.simpleframework:simple-xml:2.7.1")
    implementation("com.squareup.retrofit2:converter-simplexml:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.android.material:material:1.13.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0") //알람 기능에도 사용
    implementation ("com.github.bumptech.glide:glide:4.12.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.12.0")

    // 알람 기능 관련
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")

    // Firebase 관련
    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-analytics")
    // Google 로그인을 위한 라이브러리
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    // 카카오 로그인 SDK
    implementation("com.kakao.sdk:v2-user:2.22.0")
    // Firestore
    implementation("com.google.firebase:firebase-firestore:24.9.0")
    // Crop
    implementation("com.github.yalantis:ucrop:2.2.8")
    //알약크기설정
    implementation("com.google.android.flexbox:flexbox:3.0.0")

}