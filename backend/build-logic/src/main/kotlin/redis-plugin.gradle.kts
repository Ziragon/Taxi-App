plugins {
    id("spring-app")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    implementation(libs.findLibrary("boot-starter-data-redis").get())
}
