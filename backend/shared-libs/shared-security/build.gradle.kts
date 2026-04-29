plugins {
    id("spring-library")
    id("security-jwt-plugin")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencyManagement {
    imports {
        val springCloudVersion = libs.findVersion("spring-cloud").get().requiredVersion
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion")
    }
}

dependencies {
    api(libs.findLibrary("boot-starter-webflux").get()) // есть во всех микросервисах
    compileOnly(libs.findLibrary("boot-starter-openfeign").get()) // только для использующих Feign
}
