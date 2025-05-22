import com.android.build.api.dsl.Packaging
import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
}

// Function to load API key (keep as is)
fun getApiKey(propertyKey: String): String {
    val properties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        properties.load(FileInputStream(localPropertiesFile))
        return properties.getProperty(propertyKey) ?: ""
    }
    return ""
}

android {
    namespace = "com.shaivites.quizion"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.shaivites.quizion"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Ensure BuildConfigField is correctly defined
        buildConfigField("String", "GEMINI_API_KEY", "\"${getApiKey("GEMINI_API_KEY")}\"")
    }
    buildFeatures {
        buildConfig = true
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
    // Add packagingOptions if encountering duplicate classes, especially with Guava
    // Exclude Guava files if necessary, but try forcing version first
    // resources.excludes.add("META-INF/google_guava/module-info.class")
    // resources.excludes.add("META-INF/versions/9/module-info.class")
    fun Packaging.() {
        resources.excludes.add("META-INF/INDEX.LIST")
        resources.excludes.add("META-INF/DEPENDENCIES")
        // Exclude Guava files if necessary, but try forcing version first
        // resources.excludes.add("META-INF/google_guava/module-info.class")
        // resources.excludes.add("META-INF/versions/9/module-info.class")
    }
}

// *** ADD THIS BLOCK TO FORCE GUAVA VERSION ***
configurations.all {
    resolutionStrategy.force("com.google.guava:guava:32.1.3-android")
    // You might need to force other related dependencies if conflicts persist
     resolutionStrategy.force("com.google.code.findbugs:jsr305:3.0.2")
     resolutionStrategy.force("org.checkerframework:checker-qual:3.12.0")
}


dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0") // Or your version
    implementation("io.github.shashank02051997:FancyToast:2.0.2")
    implementation("com.google.android.material:material:1.12.0") // Or your version
    implementation("androidx.constraintlayout:constraintlayout:2.2.1") // Or your version
    // Splash Screen API
    implementation("androidx.core:core-splashscreen:1.0.1") // Or latest stable version
    // Picasso (from your file) - Keep if needed, but Glide is also present
    implementation("com.squareup.picasso:picasso:2.71828")
    // Lottie (from your file)
    implementation("com.airbnb.android:lottie:6.6.4") // Updated to a recent version

    // Glide (Ensure consistency)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0") // Use annotationProcessor or ksp

    // Firebase (from your file - might need specific firebase libs like bom, analytics if used)
    implementation("com.google.firebase:firebase-crashlytics-buildtools:3.0.3") // This is a build tool, not usually an implementation dependency

    // Gemini SDK
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    // Gson
    implementation("com.google.code.gson:gson:2.12.1")

    // Guava (Explicitly added)
    implementation("com.google.guava:guava:33.4.7-android")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1") // Updated version
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1") // Updated version
}

