plugins {
    id("spring-app")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencyManagement {
    imports {
        val springCloudVersion = libs.findVersion("spring-cloud").get().requiredVersion
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion")
    }
}

dependencies {
    implementation(libs.findLibrary("spring-cloud-starter-gateway").get())
    implementation(libs.findLibrary("springdoc-webflux-ui").get())
}
