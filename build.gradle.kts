plugins {
    kotlin("jvm") version "1.3.72"
    id("org.jetbrains.kotlin.plugin.spring") version "1.3.72"
    id("com.github.johnrengelman.shadow") version "5.2.0"
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
    implementation("org.springframework.boot:spring-boot-starter-log4j2:2.2.2.RELEASE")

    implementation("org.yaml:snakeyaml:1.26")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "11"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "11"
    }
    shadowJar {
        transform(com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer::class.java)
        mergeServiceFiles()
        manifest {
            attributes(mapOf("Main-Class" to "name.nepavel.vacation_bot.MainKt"))
        }
    }
}

configurations.forEach { it.exclude("org.springframework.boot", "spring-boot-starter-logging") }