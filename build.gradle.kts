import io.micronaut.gradle.MicronautRuntime
import io.micronaut.gradle.MicronautTestRuntime

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.9.25"
    id("com.google.devtools.ksp") version "1.9.25-1.0.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("io.micronaut.minimal.application") version "4.4.4"
    id("io.micronaut.aot") version "4.4.4"
    id("org.graalvm.buildtools.native") version "0.10.5"
}

version = "0.1"
group = "elidemin.dev.elide"

val kotlinVersion=project.properties.get("kotlinVersion")
repositories {
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/") {
        mavenContent { snapshotsOnly() }
    }
    mavenCentral()
}

dependencies {
    ksp("io.micronaut.serde:micronaut-serde-processor")
    implementation("com.github.ajalt.clikt:clikt:5.0.1")
    implementation("io.micronaut:micronaut-aop")
    implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
    // implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlinVersion}")
    runtimeOnly("ch.qos.logback:logback-classic")

    // needed for link-at-build-time compat
    implementation("javax.inject:javax.inject:1")
    implementation("io.micronaut:micronaut-core-reactive:4.8.4")
}

application {
    mainClass = "elidemin.dev.elide.ApplicationKt"
}
java {
    sourceCompatibility = JavaVersion.toVersion("21")
}

val flags = listOf(
    "--enable-native-access=ALL-UNNAMED",
    "--enable-preview",
    "--add-modules=jdk.incubator.vector",
)

val initializeAtRuntime = listOf(
    "io.micronaut.context.env.exp.RandomPropertyExpressionResolver\$LazyInit",
)

val gvmFlags = flags.plus(listOf(
    "-H:+UnlockExperimentalVMOptions",
    "--pgo-instrument",
    "--link-at-build-time=kotlin",
    "--link-at-build-time=kotlinx",
    "--link-at-build-time=elidemin",
    "--link-at-build-time=com.github.ajalt.clikt",
    "--link-at-build-time=io.micronaut.context",
    "-H:+ForeignAPISupport",
    "--initialize-at-build-time",
    "--enable-sbom",
    "--emit=build-report",
    "--gc=G1",
    "--trace-object-instantiation=java.security.SecureRandom",
)).plus(initializeAtRuntime.map {
    "--initialize-at-run-time=$it"
})

graalvmNative {
    toolchainDetection = false
    binaries {
        named("main") {
            fallback = false
            richOutput = true
            buildArgs.addAll(gvmFlags)
            jvmArgs.addAll(flags)
        }
        named("optimized") {
            fallback = false
            richOutput = true
            buildArgs.addAll(gvmFlags)
            jvmArgs.addAll(flags)
        }
    }
}

micronaut {
    runtime(MicronautRuntime.NONE)
    testRuntime(MicronautTestRuntime.JUNIT_5)
    processing {
        incremental(true)
        annotations("elidemin.dev.elide.*")
    }
    aot {
        // Please review carefully the optimizations enabled below
        // Check https://micronaut-projects.github.io/micronaut-aot/latest/guide/ for more details
        optimizeServiceLoading = false
        convertYamlToJava = false
        precomputeOperations = false
        cacheEnvironment = false
        optimizeClassLoading = false
        deduceEnvironment = false
        optimizeNetty = false
        replaceLogbackXml = false
    }
}
