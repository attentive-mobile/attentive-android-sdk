plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

// Ensure kotlin-stdlib doesn't get bundled - it's provided by the lint runtime
configurations.all {
    if (name == "runtimeElements" || name == "apiElements") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
    }
}

dependencies {
    compileOnly(libs.lint.api)
    compileOnly(libs.lint.checks)

    testImplementation(libs.lint)
    testImplementation(libs.lint.tests)
    testImplementation(libs.junit)
}

tasks.withType<Jar> {
    manifest {
        attributes["Lint-Registry-v2"] = "com.attentive.lint.AttentiveLintRegistry"
    }
}
