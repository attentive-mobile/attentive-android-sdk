import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
    sourceSets.named("main") {
        // ScreenNameFormatter is shared with the SDK so generated and runtime names match
        kotlin.srcDir("../attentive-android-sdk/src/shared/java")
    }
}

dependencies {
    implementation(libs.ksp.api)

    testImplementation(libs.junit)
}
