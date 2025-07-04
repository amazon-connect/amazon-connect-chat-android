import java.util.Properties

val versionProperties = Properties().apply {
    file("version.properties").inputStream().use { load(it) }
}

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    `maven-publish`
    alias(libs.plugins.hilt)
    alias(libs.plugins.serialization)
    id("com.spotify.ruler")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.amazon.connect.chat.sdk"
    compileSdk = 34
    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        buildConfigField("String", "SDK_VERSION", "\"${versionProperties["sdkVersion"]}\"")
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
    compileOnly(libs.androidxCoreKtx)
    compileOnly(libs.androidxLifecycleRuntimeKtx)
    compileOnly(libs.runtimeLivedata)
    compileOnly(libs.material3)

    // Lifecycle livedata
    implementation(libs.lifecycleLivedataKtx)
    implementation(libs.lifecycleViewmodelKtx)
    implementation(libs.coroutinesAndroid)

    // Retrofit
    api(libs.retrofit)
    implementation(libs.okhttp)
    api(libs.adapterRxjava2)
    implementation(libs.converterGson)
    implementation(libs.loggingInterceptor)

    //Hilt
    compileOnly(libs.hiltAndroid)
    compileOnly(libs.lifecycleProcess)
    ksp(libs.hiltCompiler)
    ksp(libs.hiltAndroidCompiler)

    // AWS
    api(libs.awsSdkCore)
    api(libs.awsSdkConnectParticipant)

    // Serialization
    implementation(libs.serializationJson)

    // Testing
    // Mockito for mocking
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.inline)
    testImplementation(libs.material3)
    testImplementation(libs.mockk)

    // Kotlin extensions for Mockito
    testImplementation(libs.mockito.kotlin)

    // Coroutines test library
    testImplementation(libs.coroutines.test)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidxJunit)


    testImplementation(libs.robolectric)
}

// Ensure the AAR file is built before publishing
tasks.withType<AbstractPublishToMaven>().configureEach {
    dependsOn(tasks.named("assembleRelease"))
}

tasks.withType<Test>().configureEach {
    jvmArgs("--add-opens", "java.base/java.util=ALL-UNNAMED")
}


// For local publishing
// Can be used in example app like below
// Keeping group Id different for local testing purpose
// implementation("com.amazon.connect.chat.sdk:connect-chat-sdk:1.0.0")
//publishing {
//    publications {
//        // Create a MavenPublication for the release build type
//        create<MavenPublication>("release") {
//            afterEvaluate {
//                artifact(tasks.getByName("bundleReleaseAar"))
//            }
//            groupId = "com.amazon.connect.chat.sdk"
//            artifactId = "connect-chat-sdk"
//            version = "1.0.2"
//        }
//    }
//    // Define the repository where the artifact will be published
//    repositories {
//        mavenLocal()
//    }
//}


// Test summary gradle file
apply(from = "test-summary.gradle.kts")

// release file
apply(from = "release.gradle.kts")