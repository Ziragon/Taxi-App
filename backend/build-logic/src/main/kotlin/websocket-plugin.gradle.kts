plugins {
    id("spring-app")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    implementation(libs.findLibrary("spring-websocket").get())
    implementation(libs.findLibrary("spring-messaging").get())
}