plugins {
    id("spring-service-webmvc")
    id("security-jwt-plugin")
    id("database-plugin")
    id("redis-plugin")
    id("rabbitmq-plugin")
    id("integration-testing")
}

dependencies {
    implementation(project(":shared-libs:shared-exceptions"))
    implementation(project(":shared-libs:shared-security"))
}
