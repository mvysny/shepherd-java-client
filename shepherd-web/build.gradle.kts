import org.gradle.kotlin.dsl.testing
import org.gradle.kotlin.dsl.vaadin

plugins {
    alias(libs.plugins.vaadin)
    application
}

dependencies {
    implementation(project(":shepherd-java-api"))
    implementation(libs.slf4j.simple)

    implementation(libs.vaadin.core) {
        if (vaadin.effective.productionMode.get()) {
            exclude(module = "vaadin-dev")
        }
    }
    implementation(libs.vaadin.boot)
    implementation(libs.karibu.dsl)

    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
    testImplementation(libs.karibu.testing)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("MainKt")
}
