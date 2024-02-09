plugins {
    kotlin("plugin.serialization") version libs.versions.kotlin
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ztexec)
    implementation(libs.slf4j.api)
    testImplementation(libs.dynatest)
}

kotlin {
    explicitApi()
}

val configureMavenCentral = ext["configureMavenCentral"] as (artifactId: String) -> Unit
configureMavenCentral("shepherd-java-api")
