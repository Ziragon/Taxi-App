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
}

tasks.named<Test>("test") {
    description = "Runs all tests (unit and integration)."
    useJUnitPlatform()
}