plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose"); id("com.google.devtools.ksp") }
android {
 namespace = "org.lembrebem"
 compileSdk = 35
 defaultConfig { applicationId = "org.lembrebem"; minSdk = 26; targetSdk = 35; versionCode = 1; versionName = "0.1.0-test"; testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
 buildFeatures { compose = true; buildConfig = true }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
 packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
dependencies {
 implementation(platform("androidx.compose:compose-bom:2025.04.01"))
 implementation("androidx.activity:activity-compose:1.10.1")
 implementation("androidx.compose.material3:material3")
 implementation("androidx.compose.ui:ui-tooling-preview")
 debugImplementation("androidx.compose.ui:ui-tooling")
 implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
 implementation("androidx.appcompat:appcompat:1.7.0")
 implementation("androidx.core:core-ktx:1.16.0")
 implementation("androidx.room:room-runtime:2.7.1")
 implementation("androidx.room:room-ktx:2.7.1")
 ksp("androidx.room:room-compiler:2.7.1")
 implementation("androidx.datastore:datastore-preferences:1.1.7")
 implementation("androidx.work:work-runtime-ktx:2.10.1")
 implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
 implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
 implementation("com.google.firebase:firebase-auth")
 implementation("com.google.firebase:firebase-messaging")
 implementation("com.google.android.gms:play-services-location:21.3.0")
 implementation("com.google.mlkit:text-recognition:16.0.1")
 implementation("com.google.mlkit:text-recognition-japanese:16.0.1")
 implementation("androidx.exifinterface:exifinterface:1.4.1")
 implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
 testImplementation("junit:junit:4.13.2")
 androidTestImplementation("androidx.test.ext:junit:1.2.1")
 androidTestImplementation("androidx.test:runner:1.6.2")
 androidTestImplementation(platform("androidx.compose:compose-bom:2025.04.01"))
 androidTestImplementation("androidx.compose.ui:ui-test-junit4")
 debugImplementation("androidx.compose.ui:ui-test-manifest")
}
