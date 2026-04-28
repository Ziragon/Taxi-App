plugins {
    id("spring-library")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    api(libs.findLibrary("jackson-databind").get())
    api(libs.findLibrary("springdoc-webmvc-ui").get())
}
