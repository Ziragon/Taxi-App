plugins {
    id("spring-library")
    id("security-jwt-plugin")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    api(libs.findLibrary("boot-starter-webflux").get())
}
