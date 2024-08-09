plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    `maven-publish`
    alias(libs.plugins.hilt)
    alias(libs.plugins.serialization)
    id("com.spotify.ruler")
    id("kotlin-kapt") // Add this for kapt
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
    implementation("com.google.android.gms:play-services-basement:18.2.0")
    implementation(libs.runtimeLivedata)
    implementation(libs.lifecycleProcess) // Add this dependency in libs.versions.toml if necessary
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidxJunit)
    androidTestImplementation(libs.espressoCore)
    androidTestImplementation(platform(libs.composeBom))
    androidTestImplementation(libs.composeUiTestJunit4)
    debugImplementation(libs.composeUiTooling)
    debugImplementation(libs.composeUiTestManifest)

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

}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.amazon.connect.chat"
            artifactId = "library"
            version = "1.0.0"

            // Specify the AAR artifact
            artifact("$buildDir/outputs/aar/${project.name}-release.aar")
        }
    }
    repositories {
        maven {
            url = uri("$rootDir/repo")
        }
    }
}

// Ensure the AAR file is built before publishing
tasks.withType<AbstractPublishToMaven>().configureEach {
    dependsOn(tasks.named("assembleRelease"))
}
