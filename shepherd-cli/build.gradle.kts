plugins {
    application
}

dependencies {
    implementation(project(":shepherd-java-api"))
    implementation(libs.kotlinx.cli)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.slf4j.simple)

    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass = "MainKt"
}
