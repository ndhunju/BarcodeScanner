plugins {
    `maven-publish`
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.ndhunju.barcodescanner"
    compileSdk = 34

    defaultConfig {
        minSdk = 24

        aarMetadata {
            // If your library contains manifest entries or resources that
            // make use of newer platform attributes, you need to set this value
            // https://developer.android.com/build/publish-library/prep-lib-release
            minSdk = 24
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "${rootProject.extra["kotlin_compiler_ext_version"]}"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }

    // When publishing a library for others to consume, consider creating test fixtures for your API.
    // https://developer.android.com/build/publish-library/prep-lib-release
    testFixtures {
        enable = true
    }
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.ndhunju"
            artifactId = "barcode-scanner"
            version = "1.0"

            afterEvaluate {
                from(components["release"])
            }
        }

        repositories {
            maven {
                name = "ndhunju"
                url = uri("${project.buildDir}/repo")
            }
        }

    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("com.google.android.material:material:1.11.0")

    // Compose
    implementation("androidx.activity:activity-compose:${rootProject.extra["activity_compose_version"]}")
    implementation(platform("androidx.compose:compose-bom:${rootProject.extra["compose_bom_version"]}"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling:1.6.3")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose")
    implementation("androidx.lifecycle:lifecycle-runtime-compose")
    implementation("androidx.constraintlayout:constraintlayout-compose:1.0.1")

    // For QR code scanning
    implementation("com.google.mlkit:barcode-scanning:17.2.0")
    implementation("com.google.mlkit:object-detection:17.0.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}