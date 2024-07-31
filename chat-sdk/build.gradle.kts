plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    `maven-publish`
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
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidxJunit)
    androidTestImplementation(libs.espressoCore)
    androidTestImplementation(platform(libs.composeBom))
    androidTestImplementation(libs.composeUiTestJunit4)
    debugImplementation(libs.composeUiTooling)
    debugImplementation(libs.composeUiTestManifest)
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
