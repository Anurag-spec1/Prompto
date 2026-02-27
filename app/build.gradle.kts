plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.hustlers.prompto"
    compileSdk {
        version = release(36)
    }

    val apiKey: String = project.findProperty("API_KEY") as String? ?: ""

    defaultConfig {

        buildConfigField(
            "String",
            "API_KEY",
            "\"$apiKey\""
        )

        applicationId = "com.hustlers.prompto"
        minSdk = 25
        targetSdk = 36
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures{
        viewBinding=true
    }
}

dependencies {

    //carbon library for better ui
    api("tk.zielony:carbon:0.16.0.1")

    //api call(retrofit)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    //cooroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    //this one contains dispacther.main
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    //glide(for image loading)
    implementation("com.github.bumptech.glide:glide:5.0.5")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}