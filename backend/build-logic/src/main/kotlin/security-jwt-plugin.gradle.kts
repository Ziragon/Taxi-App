plugins {
    id("spring-app")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    implementation(libs.findLibrary("boot-starter-security").get())
    implementation(libs.findLibrary("jjwt-api").get())
    runtimeOnly(libs.findLibrary("jjwt-impl").get())
    runtimeOnly(libs.findLibrary("jjwt-jackson").get())
}
