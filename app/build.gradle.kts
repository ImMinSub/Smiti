plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.smiti"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.smiti"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    
    // RecyclerView 의존성 추가
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    
    // Retrofit 및 JSON 변환 라이브러리
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.google.code.gson:gson:2.10.1")
    
    // OkHttp 추가
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // SwipeRefreshLayout 라이브러리 추가
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // 웹소켓 클라이언트 지원을 위한 의존성
    implementation("org.java-websocket:Java-WebSocket:1.5.3")
    
    // Glide 이미지 로딩 라이브러리
    implementation("com.github.bumptech.glide:glide:4.16.0")
    
    // CircleImageView 라이브러리
    implementation("de.hdodenhof:circleimageview:3.1.0")
}