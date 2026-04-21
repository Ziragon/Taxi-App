plugins {
    id("base-convention")
    id("io.spring.dependency-management")
    id("java-library")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

tasks.named("jar") {
    enabled = true
}

dependencyManagement {
    imports {
        val springBootVersion = libs.findVersion("spring-boot").get().requiredVersion
        mavenBom("org.springframework.boot:spring-boot-dependencies:$springBootVersion")
    }
}

dependencies {
    compileOnly(libs.findLibrary("lombok").get())
    annotationProcessor(libs.findLibrary("lombok").get())

    testImplementation(libs.findLibrary("boot-starter-test").get())
}
