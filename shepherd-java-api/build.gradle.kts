plugins {
    kotlin("plugin.serialization") version "1.8.21"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation("org.zeroturnaround:zt-exec:1.12")
    testImplementation("com.github.mvysny.dynatest:dynatest:0.24")
}

kotlin {
    explicitApi()
}

val configureMavenCentral = ext["configureMavenCentral"] as (artifactId: String) -> Unit
configureMavenCentral("shepherd-java-api")
