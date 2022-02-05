import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.31"
}

group = "org.goticks"
version = "1.0-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_1_8

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("com.typesafe.akka:akka-actor-typed_2.12:2.6.18")
    implementation("com.typesafe.akka:akka-testkit_2.12:2.6.18")
    implementation("com.typesafe.akka:akka-stream_2.12:2.6.18")
    implementation("com.typesafe.akka:akka-slf4j_2.12:2.6.18")
    implementation("ch.qos.logback:logback-classic:1.2.10")
    implementation("com.typesafe.akka:akka-stream-testkit_2.12:2.6.18")
    implementation("com.typesafe.akka:akka-http_2.12:10.2.7")
    implementation("com.typesafe.akka:akka-http-testkit_2.12:10.2.7")
    implementation("com.typesafe.akka:akka-http-jackson_2.12:10.2.7")
    implementation("com.typesafe.akka:akka-http-jackson_2.12:10.2.7")

    // https://mvnrepository.com/artifact/com.fasterxml.jackson.module/jackson-module-kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.1")

}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}
