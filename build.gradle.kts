plugins {
    kotlin("jvm") version "2.1.21"
    kotlin("plugin.serialization") version "1.8.0"
    application
}

group = "com.zendril"
version = "0.1.0"

repositories {
    mavenCentral()
    maven { url = uri("https://repository.jboss.org/") }
    maven { url = uri("https://maven.google.com") } // Google's Maven repository
}

dependencies {
    // Kotlin standard library and coroutines
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

    // HTTP client
    implementation("io.ktor:ktor-client-core:2.2.3")
    implementation("io.ktor:ktor-client-cio:2.2.3")
    implementation("io.ktor:ktor-client-content-negotiation:2.2.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.2.3")

    // HTML parsing
    implementation("org.jsoup:jsoup:1.15.3")

    // JSON processing
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")

    // YouTube Data API
    implementation("com.google.apis:google-api-services-youtube:v3-rev20220926-2.0.0")
    implementation("com.google.api-client:google-api-client:2.0.0")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.34.1")

    // Reddit API (JRAW)
    implementation("net.dean.jraw:JRAW:1.1.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")

    // RSS parsing
    implementation("com.rometools:rome:1.18.0")

    // Google Cloud Vertex AI for Gemini API access (JVM-compatible)
    implementation("com.google.cloud:google-cloud-aiplatform:3.50.0")
    implementation("com.google.cloud:google-cloud-core:2.27.0")
    implementation("com.google.cloud:google-cloud-vertexai:0.8.0")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.6")
    implementation("ch.qos.logback:logback-classic:1.4.5")

    // Testing
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("io.mockk:mockk:1.13.4")
}

application {
    mainClass.set("com.zendril.newsgregator.MainKt")
}

tasks.test {
    useJUnitPlatform()
}
