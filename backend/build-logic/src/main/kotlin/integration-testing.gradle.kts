plugins {
    id("spring-app")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    testImplementation(libs.findLibrary("boot-testcontainers").get())
    testImplementation(libs.findLibrary("testcontainers-junit-jupiter").get())
    testImplementation(libs.findLibrary("testcontainers-postgresql").get())
    testImplementation(libs.findLibrary("testcontainers-rabbitmq").get())
    testImplementation(libs.findLibrary("testcontainers-redis").get())
    testImplementation(libs.findLibrary("boot-webmvc-test").get())
    testImplementation(libs.findLibrary("boot-starter-security-test").get())
    testImplementation(libs.findLibrary("boot-starter-jpa-test").get())
    testImplementation(libs.findLibrary("testcontainers-jdbc").get())
    testImplementation(libs.findLibrary("awaitility").get())
}

tasks.named<Test>("test") {
    description = "Runs all tests (unit and integration)."
    maxParallelForks = 1
    useJUnitPlatform()
}