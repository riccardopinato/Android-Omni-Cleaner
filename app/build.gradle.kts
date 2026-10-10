plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.riccardopinato.omnicleaner"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.riccardopinato.omnicleaner"
        minSdk = 24
        targetSdk = 36
        versionCode = 4
        versionName = "0.4.0-recovery"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val testAdmobAppId = "ca-app-pub-3940256099942544~3347511713"
    val testBannerUnitId = "ca-app-pub-3940256099942544/6300978111"
    val testInterstitialUnitId = "ca-app-pub-3940256099942544/1033173712"

    val releaseAdmobAppId = providers.gradleProperty("OMNI_ADMOB_APP_ID").orNull
    val releaseBannerUnitId = providers.gradleProperty("OMNI_ADMOB_BANNER_ID").orNull
    val releaseInterstitialUnitId = providers.gradleProperty("OMNI_ADMOB_INTERSTITIAL_ID").orNull
    val releaseAdsConfigured = listOf(
        releaseAdmobAppId,
        releaseBannerUnitId,
        releaseInterstitialUnitId,
    ).all { !it.isNullOrBlank() }

    fun quoted(value: String): String = "\"$value\""

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isDebuggable = true
            manifestPlaceholders["admobAppId"] = testAdmobAppId
            buildConfigField("boolean", "ADS_ENABLED", "true")
            buildConfigField("String", "ADMOB_BANNER_UNIT_ID", quoted(testBannerUnitId))
            buildConfigField("String", "ADMOB_INTERSTITIAL_UNIT_ID", quoted(testInterstitialUnitId))
        }

        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            manifestPlaceholders["admobAppId"] = releaseAdmobAppId ?: testAdmobAppId
            buildConfigField("boolean", "ADS_ENABLED", releaseAdsConfigured.toString())
            buildConfigField("String", "ADMOB_BANNER_UNIT_ID", quoted(releaseBannerUnitId.orEmpty()))
            buildConfigField("String", "ADMOB_INTERSTITIAL_UNIT_ID", quoted(releaseInterstitialUnitId.orEmpty()))
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
        warningsAsErrors = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("androidx.documentfile:documentfile:1.1.0")
    implementation("androidx.exifinterface:exifinterface:1.4.2")
    implementation("com.android.billingclient:billing:9.1.0")
    implementation("com.google.android.gms:play-services-ads:25.5.0")
    implementation("com.google.android.ump:user-messaging-platform:4.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
}
