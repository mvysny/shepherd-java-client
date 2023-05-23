dependencies {
    api(kotlin("stdlib-jdk8"))
    testImplementation("com.github.mvysny.dynatest:dynatest:0.24")
}

kotlin {
    explicitApi()
}

val configureMavenCentral = ext["configureMavenCentral"] as (artifactId: String) -> Unit
configureMavenCentral("shepherd-java-api")
