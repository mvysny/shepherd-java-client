plugins {
    kotlin("plugin.serialization") version libs.versions.kotlin
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ztexec)
    implementation(libs.slf4j.api)
    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
    testImplementation(libs.slf4j.simple)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    explicitApi()
}

val configureMavenCentral = ext["configureMavenCentral"] as (artifactId: String) -> Unit
configureMavenCentral("shepherd-java-api")
