import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.1.21"
    id("org.springframework.boot") version "3.2.2"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("plugin.spring") version "1.9.25"
    id("net.mayope.deployplugin") version ("0.0.65")
}
repositories {
    mavenCentral()
}
deploy {
    default {
        serviceName = "solarman"

        fun Project.loadFromSettingsGradle(key: String) =
            project.findProperty(key) as String? ?: error(
                "You have to set $key in settings.gradle before using this project"
            )
        dockerBuild {
            buildOutputTask = "bootJar"
        }
        dockerLogin {
            registryRoot = "https://index.docker.io/v1/"
            loginMethod = net.mayope.deployplugin.tasks.DockerLoginMethod.CLASSIC
            loginUsername = project.loadFromSettingsGradle("dockerHubRegistryUser")
            loginPassword = project.loadFromSettingsGradle("dockerHubRegistryPassword")
        }
        dockerPush {
            registryRoot = "mayope"
            loginMethod = net.mayope.deployplugin.tasks.DockerLoginMethod.CLASSIC
            loginUsername = project.loadFromSettingsGradle("dockerHubRegistryUser")
            loginPassword = project.loadFromSettingsGradle("dockerHubRegistryPassword")
        }

        deploy {
            kubeConfig = System.getProperty("user.home") + "/.kube/config-staudens-reihe"
            targetNamespaces = listOf("homeassistant")
        }
    }
}

group = "de.klg71.solar"
version = "0.0.1-SNAPSHOT"

dependencies {
    kotlin("stdlib")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    implementation("io.github.openfeign:feign-core:13.6")
    implementation("io.github.openfeign:feign-jackson:13.6")

    runtimeOnly("com.h2database:h2")
}
tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
    }
}
