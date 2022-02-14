plugins {
    kotlin("jvm") version "1.6.10"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val akkaVersion = "2.6.18"
val akkaHttpVersion = "10.2.7"
val scalaBinary = "2.12"

dependencies {
    implementation(kotlin("stdlib"))

    implementation("com.typesafe.akka:akka-actor-typed_$scalaBinary:$akkaVersion")
    implementation("com.typesafe.akka:akka-stream-typed_$scalaBinary:$akkaVersion")

    implementation("com.typesafe.akka:akka-persistence-typed_$scalaBinary:$akkaVersion")
    implementation("com.typesafe.akka:akka-persistence-query_$scalaBinary:$akkaVersion")
    implementation("org.iq80.leveldb:leveldb:0.12")
    implementation("org.fusesource.leveldbjni:leveldbjni-all:1.8")

    implementation("com.typesafe.akka:akka-cluster-typed_$scalaBinary:$akkaVersion") // contains akka-cluster-tools
    implementation("com.typesafe.akka:akka-cluster-sharding-typed_$scalaBinary:$akkaVersion")

    implementation("com.typesafe.akka:akka-http-core_$scalaBinary:$akkaHttpVersion")
    implementation("com.typesafe.akka:akka-http_$scalaBinary:$akkaHttpVersion")
    implementation("com.typesafe.akka:akka-http-jackson_$scalaBinary:$akkaHttpVersion")

    testImplementation("com.typesafe.akka:akka-testkit_$scalaBinary:$akkaVersion")
    testImplementation("com.typesafe.akka:akka-multi-node-testkit_$scalaBinary:$akkaVersion")

    implementation("com.typesafe.akka:akka-slf4j_$scalaBinary:$akkaVersion")
    implementation("ch.qos.logback:logback-classic:1.2.10")

    implementation(platform("com.typesafe.akka:akka-bom_$scalaBinary:$akkaVersion"))
    implementation("com.typesafe.akka:akka-serialization-jackson_$scalaBinary:$akkaVersion")
}