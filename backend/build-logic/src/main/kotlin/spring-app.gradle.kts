plugins {
    id("base-convention")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencyManagement {
    imports {
        val springBootVersion = libs.findVersion("spring-boot").get().requiredVersion
        mavenBom("org.springframework.boot:spring-boot-dependencies:$springBootVersion")
    }
}

dependencies {
    implementation(libs.findLibrary("boot-starter").get())
    implementation(libs.findLibrary("boot-starter-actuator").get())
    implementation(libs.findLibrary("boot-starter-validation").get())

    compileOnly(libs.findLibrary("lombok").get())
    annotationProcessor(libs.findLibrary("lombok").get())
    annotationProcessor(libs.findLibrary("boot-configuration-processor").get())

    testImplementation(libs.findLibrary("boot-starter-test").get())
}
