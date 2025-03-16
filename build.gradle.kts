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
    maven("https://repo.e-iceblue.cn/repository/maven-public/")
}

application {
    mainClass = "com.lavaloon.zatca.MainKt"
    // The ConvertPdfCommand generates invalid dates on Arabic locales if the JVM language isn't English
    applicationDefaultJvmArgs = listOf("-Duser.language=en", "-Duser.timezone=Asia/Riyadh")
}

buildConfig {
    buildConfigField("APP_VERSION", provider { "${project.version}" })
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.jcommander:jcommander:1.83")
    implementation(files("lib/zatca-einvoicing-sdk-238-R3.3.9.jar"))
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    implementation("org.apache.pdfbox:pdfbox:2.0.29")
    implementation("org.apache.pdfbox:pdfbox-tools:2.0.29")
    implementation("e-iceblue:spire.pdf.free:9.13.0")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(11)
}
