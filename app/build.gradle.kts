plugins { id("com.android.application") }

android { namespace = "com.example.dynamicisland"; compileSdk = 37
    defaultConfig { applicationId = "com.example.dynamicisland"; minSdk = 26; targetSdk = 37; versionCode = 1; versionName = "1.0.0" }
    buildTypes { release { isMinifyEnabled = true; proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro") } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.2")
    implementation("androidx.dynamicanimation:dynamicanimation:1.1.0")
    implementation("androidx.palette:palette-ktx:1.0.0")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.media:media:1.7.0")
}
