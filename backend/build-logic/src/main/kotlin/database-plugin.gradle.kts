plugins {
    id("spring-app")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    implementation(libs.findLibrary("boot-starter-data-jpa").get())

    runtimeOnly(libs.findLibrary("postgresql").get())

    implementation(libs.findLibrary("flyway-core").get())
    implementation(libs.findLibrary("flyway-database-postgresql").get())
}
