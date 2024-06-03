plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-android")
    id("kotlin-parcelize")
    id("kotlin-kapt")
    id("maven-publish")
}

android {
    namespace = "com.prng.aeedee_android_chat"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        targetSdk = 34

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

    dataBinding.enable = true
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.github.GopikrishnanN"
            artifactId = "Aeedee-Android-Chat"
            version = "1.0"
        }
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // - - Retrofit2
    val retrofitVersion = "2.9.0"
    val loggingVersion = "4.9.0"
    val okHttpVersion = "4.12.0"
    implementation("com.squareup.retrofit2:retrofit:$retrofitVersion")
    implementation("com.squareup.retrofit2:converter-gson:$retrofitVersion")
    implementation("com.squareup.okhttp3:okhttp:$okHttpVersion")
    implementation("com.squareup.okhttp3:logging-interceptor:$loggingVersion")

    val lifecycleVersion = "2.8.0"
    // - - ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    // - - LiveData
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")

    val navVersion = "2.7.7"
    // - - Nav Graph
    implementation("androidx.navigation:navigation-fragment-ktx:$navVersion")
    implementation("androidx.navigation:navigation-ui-ktx:$navVersion")

    val circleImageVersion = "3.1.0"
    // - - Circle Image
    implementation("de.hdodenhof:circleimageview:$circleImageVersion")

    val socketVersion = "2.0.0"
    // - - Socket.IO
    implementation("io.socket:socket.io-client:$socketVersion") {
        exclude(group = "org.json", module = "json")
    }

    val gsonVersion = "2.10.1"
    // - - Gson
    implementation("com.google.code.gson:gson:$gsonVersion")

    val glideVersion = "4.16.0"
    // - - Glide
    implementation("com.github.bumptech.glide:glide:$glideVersion")
    annotationProcessor("com.github.bumptech.glide:compiler:$glideVersion")

    val roomVersion = "2.6.1"
    // - - Room DB
    implementation("androidx.room:room-runtime:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    // - - Kotlin Extensions and Coroutines support for Room
    implementation("androidx.room:room-ktx:$roomVersion")

    val swipeVersion = "1.1.0"
    // - - Swipe Refresh
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:$swipeVersion")

    val toolTipVersion = "1.1.0"
    // - - ToolTip Popup
    implementation("com.github.douglasjunior:android-simple-tooltip:$toolTipVersion")

    val uCropVersion = "2.2.8-native"
    // - - U Crop
    implementation("com.github.yalantis:ucrop:$uCropVersion")
}