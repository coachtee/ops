// Pure Kotlin/JVM module. Deliberately NOT an Android module: no AGP plugin,
// no Android dependency of any kind. This is the one part of the Android app
// that this sandbox can actually compile and test (`./gradlew :core-domain:test`),
// so the VAT math and sync-decision logic that MUST be correct live here,
// independent of anything the Android SDK provides.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    // Not using `jvmToolchain(17)`: that triggers Gradle's toolchain
    // auto-provisioning, which needs network access to a JDK download
    // service this sandbox does not have (only JDK 21 is installed here).
    // Targeting JVM bytecode 17 directly works fine from a JDK 21 compiler
    // without provisioning a separate toolchain, and keeps this module
    // consistent with the `app` module's JVM target of 17.
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    testImplementation(libs.junit)
}

tasks.test {
    useJUnit()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}
