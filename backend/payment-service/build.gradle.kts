plugins {
    id("spring-service-webmvc")
    id("security-jwt-plugin")
    id("database-plugin")
    id("redis-plugin")
    id("rabbitmq-plugin")
    id("openfeign-plugin")
    id("integration-testing")
    id("stripe-plugin")
}

dependencies {
    implementation(project(":shared-libs:shared-exceptions"))
    implementation(project(":shared-libs:shared-security"))
    implementation(project(":shared-libs:shared-dto"))
}