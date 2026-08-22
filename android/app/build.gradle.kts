plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    // Renders the real production screen Composables (via the XScreenContent
    // functions each screen exposes) with hand-built fake state, on the JVM,
    // no emulator/SDK needed — this is how the visual screenshot pack is
    // produced; see android/README.md and app/src/test/kotlin/.../screenshots/.
    alias(libs.plugins.paparazzi)
}

android {
    namespace = "com.ops.app"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.ops.app"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"

        // BASE_URL's debug default points at the Android emulator's alias for
        // the host machine's localhost — right for `gradle :app:assembleDebug`
        // run against an emulator. A physical device can't resolve 10.0.2.2,
        // and can't easily get a custom build either (this APK usually comes
        // from CI, not a local `gradle` invocation with a custom property) —
        // for that case there's a runtime escape hatch instead of a build-time
        // one: Settings > Developer options (debug builds only) lets a tester
        // type their machine's LAN IP straight into the installed APK; see
        // DevServerUrlInterceptor. This buildConfigField is only ever the
        // *default* debug builds fall back to when no override is set.
        buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8000/\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            // No real production domain exists yet (see android/README.md's
            // "API configuration" section) — never hardcode a guessed one
            // here. Supply the real HTTPS endpoint at build time via
            // -PopsProdApiBaseUrl=https://api.example.com/ or the
            // OPS_PROD_API_BASE_URL env var (e.g. a CI secret).
            //
            // This block is configuration-time Gradle code, so it runs on
            // every invocation touching this module — including a plain
            // `:app:assembleDebug` — whether or not a release variant is
            // actually being built. It can't safely `error()` when
            // unconfigured without breaking every debug build too. Instead,
            // an unconfigured release build falls back to a `.invalid`
            // hostname (reserved by RFC 2606 to never resolve), so it fails
            // loudly with a DNS error at connection time rather than
            // silently reaching some other real host.
            val prodBaseUrl = (project.findProperty("opsProdApiBaseUrl") as String?)
                ?: System.getenv("OPS_PROD_API_BASE_URL")
                ?: "https://ops-production-api-not-configured.invalid/"
            require(prodBaseUrl.startsWith("https://")) {
                "opsProdApiBaseUrl/OPS_PROD_API_BASE_URL must be an https:// URL, got: $prodBaseUrl"
            }
            buildConfigField("String", "BASE_URL", "\"$prodBaseUrl\"")
        }
        debug {
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":core-domain"))

    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.activity.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)

    implementation(libs.navigation.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization.converter)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.work.runtime.ktx)

    implementation(libs.datastore.preferences)

    implementation(libs.coil.compose)

    testImplementation(libs.junit)
}
