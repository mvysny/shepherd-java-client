plugins {
    application
}

dependencies {
    implementation(project(":shepherd-java-api"))
    implementation(libs.kotlinx.cli)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.slf4j.simple)
    testImplementation(libs.dynatest)
}

application {
    mainClass.set("MainKt")
}
