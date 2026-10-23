import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.hilt)
    alias(libs.plugins.serialization)
    alias(libs.plugins.composeCompiler)
    id("com.spotify.ruler")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.amazon.connect.chat.androidchatexample"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.amazon.connect.chat.androidchatexample"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isDebuggable = false
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
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // OkHttp 5 artifacts such as logging-interceptor and okhttp-coroutines (used by the AWS SDK
            // for Kotlin) each ship this OSGi manifest, so packaging fails on the duplicate.
            excludes += "/META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
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
    implementation(libs.play.services.basement)
    implementation(libs.runtimeLivedata) // Add this dependency in libs.versions.toml if necessary
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidxJunit)
    androidTestImplementation(libs.espressoCore)
    androidTestImplementation(platform(libs.composeBom))
    androidTestImplementation(libs.composeUiTestJunit4)
    debugImplementation(libs.composeUiTooling)
    debugImplementation(libs.composeUiTestManifest)
    implementation(libs.material.icons.extended)


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
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    ksp(libs.hiltCompiler)
    implementation("androidx.navigation:navigation-compose:2.7.7")
    ksp(libs.hiltAndroidCompiler)

    coreLibraryDesugaring(libs.desugarJdkLibs)

    // Serialization
    implementation(libs.serializationJson)

    // Image loading
    implementation(libs.coilCompose)

    // Pull to refresh
    implementation(libs.accompanist.swiperefresh)

    // Chat SDK
    implementation(project(":chat-sdk"))
}

ruler {
    abi.set("arm64-v8a")
    locale.set("en")
    screenDensity.set(480)
    sdkVersion.set(33)
}
