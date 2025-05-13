plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = "eu.vendeli"
version = "dev"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(libs.test.kotest.junit5)
    testImplementation(libs.test.kotest.assertions)
    testImplementation(libs.mockk)
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    compilerOptions.javaParameters = true
    jvmToolchain(22)
}
