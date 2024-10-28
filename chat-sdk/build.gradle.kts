plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    `maven-publish`
    alias(libs.plugins.hilt)
    alias(libs.plugins.serialization)
    id("com.spotify.ruler")
    id("kotlin-kapt")
}

android {
    namespace = "com.amazon.connect.chat.sdk"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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

    buildFeatures {
        compose = true
    }

    composeOptions {
        // https://developer.android.com/jetpack/androidx/releases/compose-kotlin#pre-release_kotlin_compatibility
        kotlinCompilerExtensionVersion = "1.5.4"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    publishing {
        singleVariant("release") {
            withJavadocJar()
            withSourcesJar()
        }
    }
}

dependencies {
    implementation(libs.androidxCoreKtx)
    implementation(libs.androidxLifecycleRuntimeKtx)
    implementation(libs.androidxActivityCompose)
    implementation(platform(libs.composeBom))
    implementation(libs.composeUi)
    implementation(libs.composeUiGraphics)
    implementation(libs.composeUiToolingPreview)
    implementation(libs.material3)
    implementation(libs.runtimeLivedata)

    // Lifecycle livedata
    implementation(libs.lifecycleLivedataKtx)
    implementation(libs.lifecycleViewmodelKtx)
    implementation(libs.coroutinesAndroid)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.converterGson)
    implementation(libs.okhttp)
    implementation(libs.loggingInterceptor)
    implementation(libs.otto)
    implementation(libs.adapterRxjava2)

    //Hilt
    implementation(libs.hiltAndroid)
    implementation(libs.hiltNavigationCompose)
    implementation(libs.lifecycleProcess)
    kapt(libs.hiltCompiler)
    implementation(libs.navigationCompose)
    kapt(libs.hiltAndroidCompiler)

    // AWS
    implementation(libs.awsSdkCore)
    implementation(libs.awsSdkConnectParticipant)

    // Serialization
    implementation(libs.serializationJson)

    // Image loading
    implementation(libs.coilCompose)

    // Testing
    // Mockito for mocking
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.inline)

    // Kotlin extensions for Mockito
    testImplementation(libs.mockito.kotlin)

    // Coroutines test library
    testImplementation(libs.coroutines.test)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidxJunit)
    androidTestImplementation(platform(libs.composeBom))
    androidTestImplementation(libs.composeUiTestJunit4)
    debugImplementation(libs.composeUiTooling)
    debugImplementation(libs.composeUiTestManifest)
    testImplementation(libs.robolectric)
}

// Ensure the AAR file is built before publishing
tasks.withType<AbstractPublishToMaven>().configureEach {
    dependsOn(tasks.named("assembleRelease"))
}

// For local publishing
// Can be used in example app like below
// Keeping group Id different for local testing purpose
// implementation("com.amazon.connect.chat.sdk:connect-chat-sdk:1.0.0")
publishing {
    publications {
        // Create a MavenPublication for the release build type
        create<MavenPublication>("release") {
            afterEvaluate {
                artifact(tasks.getByName("bundleReleaseAar"))
            }
            groupId = "com.amazon.connect.chat.sdk"
            artifactId = "connect-chat-sdk"
            version = "1.0.0"


        }
    }
    // Define the repository where the artifact will be published
    repositories {
        mavenLocal()
    }
}


// Test summary gradle file
apply(from = "test-summary.gradle.kts")

// release file
apply(from = "release.gradle.kts")