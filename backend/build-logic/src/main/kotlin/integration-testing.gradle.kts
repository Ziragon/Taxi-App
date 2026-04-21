plugins {
    id("spring-app")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

val sourceSets = the<SourceSetContainer>()

val integrationTestSourceSet = sourceSets.create("integrationTest") {
    compileClasspath += sourceSets["main"].output + configurations["testCompileClasspath"]
    runtimeClasspath += output + compileClasspath + configurations["testRuntimeClasspath"]
}

configurations[integrationTestSourceSet.implementationConfigurationName].extendsFrom(configurations["testImplementation"])
configurations[integrationTestSourceSet.runtimeOnlyConfigurationName].extendsFrom(configurations["testRuntimeOnly"])

dependencies {
    add(integrationTestSourceSet.implementationConfigurationName, libs.findLibrary("boot-testcontainers").get())

    add(integrationTestSourceSet.implementationConfigurationName, libs.findLibrary("testcontainers-junit-jupiter").get())
    add(integrationTestSourceSet.implementationConfigurationName, libs.findLibrary("testcontainers-postgresql").get())
    add(integrationTestSourceSet.implementationConfigurationName, libs.findLibrary("testcontainers-rabbitmq").get())
    add(integrationTestSourceSet.implementationConfigurationName, libs.findLibrary("testcontainers-redis").get())
}

tasks.register<Test>("integrationTest") {
    description = "Runs integration tests."
    group = "verification"
    testClassesDirs = integrationTestSourceSet.output.classesDirs
    classpath = integrationTestSourceSet.runtimeClasspath
    useJUnitPlatform()
}

tasks.named("check") {
    dependsOn("integrationTest")
}
