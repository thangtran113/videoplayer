plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}
android {
    namespace = "com.android19.videoplayer"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.android19.videoplayer"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
//        freeCompilerArgs = ['-Xjvm-default=compatibility']
    }
    buildFeatures{
        viewBinding = true
    }




}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.cronet.embedded)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
//    implementation("androidx.media3:media3-exoplayer:1.3.1")
//    implementation("androidx.media3:media3-exoplayer-dash:1.3.1")
//    implementation("androidx.media3:media3-ui:1.3.1")
//    implementation ("androidx.media2:media2-session:1.2.0")

    //Get video thumbail
    implementation (libs.glide)
    implementation ("com.google.android.material:material:1.5.0")
    //Double Tap
    implementation ("com.github.vkay94:DoubleTapPlayerView:1.0.4")

    //Player

    implementation ("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0-alpha01")
    //SeekBar ( old )
    implementation ("com.github.lukelorusso:VerticalSeekBar:1.2.7")

    implementation ("androidx.core:core-ktx:1.7.0")
    implementation ("androidx.media:media:1.2.1")

    implementation ("androidx.media3:media3-exoplayer-dash:1.3.1")
    // dependency for exoplayer

    implementation ("com.google.android.exoplayer:exoplayer:2.19.1")
//
//// for core support in exoplayer.
//
//    implementation ("com.google.android.exoplayer:exoplayer-core:r2.4.0")
//
//// for adding dash support in our exoplayer.
//
//    implementation ("com.google.android.exoplayer:exoplayer-dash:r2.4.0’")
//
//// for adding hls support in exoplayer.
//
//    implementation ("com.google.android.exoplayer:exoplayer-hls:r2.4.0")
//
//// for smooth streaming of video in our exoplayer.
//
//    implementation ("com.google.android.exoplayer:exoplayer-smoothstreaming:r2.4.0")
//
//// for generating default ui of exoplayer
//
//    implementation ("com.google.android.exoplayer:exoplayer-ui:r2.4.0")
}
