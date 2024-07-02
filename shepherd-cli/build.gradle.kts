plugins {
    application
}

dependencies {
    implementation(project(":shepherd-java-api"))
    implementation(libs.kotlinx.cli)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.slf4j.simple)
    testImplementation(libs.dynatest)
    testRuntimeOnly(libs.junit.platform.launcher)
}

application {
    mainClass.set("MainKt")
}
