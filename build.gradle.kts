plugins {
    kotlin("jvm") version "2.1.0"
    id("me.champeau.jmh") version "0.7.2"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Kotlin coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // File watching library
    implementation("io.methvin:directory-watcher:0.19.0")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.4.11")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("org.mockito:mockito-core:5.10.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")

    // JMH - Java Microbenchmark Harness
    jmh("org.openjdk.jmh:jmh-core:1.37")
    jmh("org.openjdk.jmh:jmh-generator-annprocess:1.37")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(22)
}

jmh {
    // JMH configuration
    iterations = 5 // Number of measurement iterations to do
    warmupIterations = 3 // Number of warmup iterations to do
    fork = 2 // How many times to fork a single benchmark
    failOnError = true // Should JMH fail immediately if any benchmark had experienced the unrecoverable error?
    forceGC = true // Should JMH force GC between iterations?
    includeTests = false // Should JMH include tests in the generated jar?
    resultFormat = "JSON" // Result format type (one of CSV, JSON, NONE, SCSV, TEXT)
    resultsFile = project.file("${project.buildDir}/reports/jmh/results.json") // Results file
    jvmArgs = listOf("-Xms2g", "-Xmx2g") // Custom JVM args for the forked benchmark JVM
}
