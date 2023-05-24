plugins {
    application
}

dependencies {
    implementation(project(":shepherd-java-api"))
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.5")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    testImplementation("com.github.mvysny.dynatest:dynatest:0.24")
}

application {
    mainClass.set("MainKt")
}
