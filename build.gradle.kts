plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    id("com.github.gmazzo.buildconfig") version "5.4.0"
    application
}

group = "com.lavaloon"
version = "2.4.0"

repositories {
    mavenCentral()
}

application {
    mainClass = "com.lavaloon.zatca.MainKt"
}

buildConfig {
    buildConfigField("APP_VERSION", provider { "${project.version}" })
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.jcommander:jcommander:1.83")
    implementation(files("lib/zatca-einvoicing-sdk-238-R3.3.9.jar"))
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(11)
}
