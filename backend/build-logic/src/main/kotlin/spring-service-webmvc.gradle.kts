plugins {
    id("spring-app")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    implementation(libs.findLibrary("boot-starter-web").get())
    implementation(libs.findLibrary("springdoc-webmvc-ui").get())
}
