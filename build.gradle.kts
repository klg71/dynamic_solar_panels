import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.3.0-Beta2"
    id("org.springframework.boot") version "3.4.2"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("plugin.spring") version "1.9.25"
    id("net.mayope.deployplugin") version ("0.0.75")
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

    implementation("io.projectreactor:reactor-core:3.7.9")
    runtimeOnly("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.10.2")
    implementation("org.reactivestreams:reactive-streams:1.0.4")

    implementation("io.github.davidepianca98:kmqtt-common-jvm:1.0.0")
    implementation("io.github.davidepianca98:kmqtt-client-jvm:1.0.0")

    implementation("io.github.openfeign:feign-core:13.6")
    implementation("io.github.openfeign:feign-jackson:13.6")

    implementation("io.github.davidepianca98:kmqtt-common:1.0.0")
    implementation("io.github.davidepianca98:kmqtt-client:1.0.0")

    runtimeOnly("com.h2database:h2")
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java).all {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_24)
    }
}
java {
    sourceCompatibility = JavaVersion.VERSION_24
    targetCompatibility = JavaVersion.VERSION_24
}
