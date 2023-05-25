plugins {
    application
}

dependencies {
    implementation(project(":shepherd-java-api"))
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.5")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation("org.slf4j:slf4j-simple:2.0.6")
    testImplementation("com.github.mvysny.dynatest:dynatest:0.24")
}

application {
    mainClass.set("MainKt")
}
