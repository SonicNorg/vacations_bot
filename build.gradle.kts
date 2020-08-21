plugins {
    kotlin("jvm") version "1.3.72"
    id("org.jetbrains.kotlin.plugin.spring") version "1.3.72"
}

group = "name.nepavel"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.telegram:telegrambots-spring-boot-starter:4.9.1")
    implementation("org.telegram:telegrambots-abilities:4.9.1")
    implementation("org.jetbrains.kotlinx:kotlinx-html:0.7.2")

    implementation("org.yaml:snakeyaml:1.26")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "11"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "11"
    }
}