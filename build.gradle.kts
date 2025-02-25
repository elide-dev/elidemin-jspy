import org.graalvm.buildtools.gradle.tasks.BuildNativeImageTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.dsl.jvm.JvmTargetValidationMode
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jetbrains.kotlin.gradle.utils.extendsFrom
import proguard.gradle.ProGuardTask

plugins {
    java
    application
    id("org.jetbrains.kotlin.jvm") version "2.1.20-RC"
    id("org.jetbrains.kotlin.plugin.allopen") version "2.1.20-RC"
    id("com.google.devtools.ksp") version "2.1.20-RC-1.0.30"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.graalvm.buildtools.native") version "0.10.5"
    id("com.gradleup.gr8") version "0.11.2"
}

buildscript {
    dependencies {
        classpath("com.guardsquare:proguard-gradle:7.6.1")
    }
}

version = "0.1"
group = "elidemin.dev.elide"

val kotlinVersion = properties["kotlinVersion"] as String
val kotlinxCoroutines = "1.10.1"
val nettyVersion = "4.1.118.Final"
val micrometerVersion = "1.1.2"
val jacksonVersion = "2.18.2"
val graalvmVersion = "24.1.2"
val jvmToolchain = 23
val jvmTarget = jvmToolchain
val kotlinTarget = JvmTarget.JVM_23
val javaTarget = JavaLanguageVersion.of(jvmToolchain)
val jvmTargetVersion = JavaVersion.toVersion(jvmTarget)
val march = "x86-64-v4"
val optMode = "s"

fun JavaToolchainSpec.javaToolchainSuite() {
    languageVersion = javaTarget
    vendor = JvmVendorSpec.ORACLE
}

val enablePgo = false
val enablePgoInstrument = false
val strict = false
val enableClang = false
val enableLlvm = false
val enableGccEdge = true
val enableSbom = false
val enableMinifier = true
val enableDualMinify = true
val enableLld = enableClang
val enableMold = true
val enableMemoryProtectionKeys = true
val enableG1 = !enableMemoryProtectionKeys
val enableExperimental = false
val enableTruffle = true
val enableAuxCache = false
val minifierMode = "gr8"
val compilerBackend = if (enableLlvm) "llvm" else "lir"
val gc = if (enableG1 && !(enableAuxCache && enableTruffle)) "G1" else "serial"
val entrypoint = "elidemin.dev.elide.ApplicationKt"

val javac = javaToolchains.compilerFor { javaToolchainSuite() }
val java = javaToolchains.launcherFor { javaToolchainSuite() }

val kotlincFlags = listOf(
    "-no-reflect",
    "-no-stdlib",
    "-Xjsr305=strict",
    "-Xabi-stability=stable",
    "-Xassertions=jvm",
    "-Xcompile-java",
    "-Xemit-jvm-type-annotations",
    "-Xir-inliner",
    "-Xenhance-type-parameter-types-to-def-not-null",
    "-Xjdk-release=${kotlinTarget.target}",
    "-Xjspecify-annotations=strict",
    "-Xjvm-default=all",
    "-Xno-call-assertions",
    "-Xno-param-assertions",
    "-Xno-receiver-assertions",
    "-Xsam-conversions=indy",
    "-Xstring-concat=indy-with-constants",
    "-Xuse-fast-jar-file-system",
    "-Xuse-k2-kapt",
    "-Xvalidate-bytecode",
    "-Xvalue-classes",
    "-Xinline-classes",
)

val minifiedConfigurations = listOf(
    "runtimeClasspath",
)

val r8Rules = listOf(
    "common.pro",
    "r8.pro"
)

val proguardRules = listOf(
    "common.pro",
    "proguard.pro"
)

val javacFlags = listOf(
    "--enable-preview",
    "--add-modules=jdk.incubator.vector",
    "--add-modules=jdk.unsupported",
    "-da",
    "-dsa",
)

val javacOnlyFlags = listOf(
    "-g",
)

val jvmDefs = mapOf(
    "polyglotimpl.DisableVersionChecks" to "false",
)

val jvmFlags = javacFlags.plus(listOf(
    "--enable-native-access=org.graalvm.truffle,ALL-UNNAMED",
    "-XX:+UnlockExperimentalVMOptions",
    //
)).plus(jvmDefs.map {
    "-D${it.key}=${it.value}"
}).plus(
    if (jvmToolchain > 24) listOf(
        "-XX:+UseCompactObjectHeaders",
    ) else emptyList()
)

val initializeAtRuntime: List<String> = listOf(
    "kotlin.coroutines.jvm.internal.BaseContinuationImpl",
)

val excludedClassPaths: List<String> = listOf(
    "kotlinx.coroutines.debug.internal.AgentPremain",
)

val exclusionPaths: List<String> = emptyList<String>().plus(excludedClassPaths.map {
    it.replace(".", "/").let { path ->
        if (path.lowercase() != path) ("$path.class") else path
    }
})

val nativeCompilerPath =
    if (enableClang) "/usr/bin/clang-18" else {
        if (enableGccEdge) "/usr/bin/gcc-14" else "/usr/bin/gcc"
    }

val nativeLinker = when {
    enableLld || enableClang -> "lld"
    enableMold -> "mold"
    else -> "ld"
}

val nativeCompileFlags = listOf(
    //"-Os",
    "-flto",
    "-fuse-ld=$nativeLinker",
)

val nativeLinkerFlags = listOf(
    "-flto",
    "--emit-relocs",
)

val experimentalFlags = listOf(
    "-H:+VectorPolynomialIntrinsics",
    "-H:+VectorPolynomialIntrinsics",
    "-H:+OptimizeLongJumps",
    "-R:+OptimizeLongJumps",
    "-H:+LSRAOptimization",
    "-R:+LSRAOptimization",
    "-H:+UseThinLocking",
    "-H:+UseStringInlining",
    "-H:+RemoveUnusedSymbols",
    "-R:+WriteableCodeCache",
    "-H:LocalizationCompressBundles='.*'",
    "-H:+InlineGraalStubs",
    "-H:+ContextAwareInlining",
    "-H:CompilerBackend=$compilerBackend",
    "-H:SpectrePHTBarriers=NonDeoptGuardTargets",
    "-H:+ReduceCodeSize",
    "-H:+ProtectionKeys",
    "-H:CFI=SW_NONATIVE",
    //
    "-H:+ForeignAPISupport",
    "-H:+LocalizationOptimizedMode",
    "-H:-BuildOutputRecommendations",
    "-H:-ReduceImplicitExceptionStackTraceInformation",
    "-H:CStandard=C11",
    //
    // "-H:+SupportRuntimeClassLoading",
    // "-H:-ClosedTypeWorld",
).plus(
    if (jvmTarget > 24) listOf(
        "-H:+VectorAPISupport",
        "-H:+MLProfileInferenceUseGNNModel",
    ) else emptyList()
)

val gvmFlags = (if (enablePgo) listOf(
    "--pgo=/home/sam/workspace/labs/gvm-min/elidemin-purekt-truffle/default.iprof",
) else {
    if (enablePgoInstrument) listOf(
        "--pgo-instrument",
    ) else listOf(
        "-O$optMode",
    )
}).plus(listOf(
    "--trace-object-instantiation=java.util.concurrent.ForkJoinWorkerThread",
    "--gc=$gc",
    "-march=$march",
    "--verbose",
    "--initialize-at-build-time",
    "--link-at-build-time",
    "--color=always",
    "--emit=build-report",
    "-H:+UnlockExperimentalVMOptions",
    "-H:+UseCompressedReferences",
    "--static-nolibc",
    "--exact-reachability-metadata",
    "--native-compiler-path=$nativeCompilerPath",
)).plus(nativeCompileFlags.map {
    "--native-compiler-options=$it"
}.plus(nativeLinkerFlags.flatMap {
    listOf(
        "--native-compiler-options=-Wl,$it",
    )
})).plus(
    if (enableExperimental) experimentalFlags else emptyList()
).plus(
    if (enableAuxCache) listOf(
        "-H:+AuxiliaryEngineCache",
    ) else emptyList()
).plus(
    if (enableSbom) listOf(
        "--enable-sbom=cyclonedx",
    ) else emptyList()
).plus(
    initializeAtRuntime.map {
        "--initialize-at-run-time=$it"
    }
)

val truffle: Configuration by configurations.creating {
    isCanBeResolved = true
}

configurations.compileClasspath.extendsFrom(
    configurations.named("truffle")
)

dependencies {
    implementation("com.github.ajalt.clikt:clikt:5.0.1")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutines")
    compileOnly("org.graalvm.nativeimage:svm:$graalvmVersion")
    truffle("org.graalvm.js:js-language:$graalvmVersion")
    truffle("org.graalvm.nativeimage:truffle-runtime-svm:$graalvmVersion")
    truffle("org.graalvm.truffle:truffle-enterprise:$graalvmVersion")
}

application {
    mainClass = entrypoint
}

java {
    sourceCompatibility = jvmTargetVersion
    targetCompatibility = jvmTargetVersion
    toolchain { javaToolchainSuite() }
    modularity.inferModulePath = true
}

kotlin {
    compilerOptions {
        apiVersion = KotlinVersion.KOTLIN_2_3
        languageVersion = KotlinVersion.KOTLIN_2_3
        jvmTarget = kotlinTarget
        javaParameters = true
        allWarningsAsErrors = strict
        freeCompilerArgs = kotlincFlags.plus(javacFlags.plus(javacOnlyFlags).map {
            "-Xjavac-arguments=$it"
        })
    }
}

tasks.shadowJar {
    exclusionPaths.forEach {
        exclude(it)
    }
}

val proguardedJar by tasks.registering(ProGuardTask::class) {
    group = "build"
    description = "Builds the proguarded jar"

    configuration(files(*proguardRules.toTypedArray()))

    dependsOn(tasks.shadowJar)
    injars(tasks.shadowJar.get().outputs.files.singleFile)
    libraryjars(truffle.files)
    val out = layout.buildDirectory.file("libs/${project.name}-proguarded.jar").get().asFile
    outjars(out)
    outputs.files(out)
}

val gr8MinifiedJar = gr8.create("minified") {
    r8Rules.forEach { proguardFile(layout.projectDirectory.file(it).asFile) }
    systemClassesToolchain(javac.get())
    r8Version(defaultR8Version)
    addClassPathJarsFrom(truffle)

    if (enableDualMinify) {
        addProgramJarsFrom(proguardedJar)
    } else {
        addProgramJarsFrom(tasks.shadowJar)
    }
}

val minifiedJar by tasks.registering(Copy::class) {
    group = "build"
    description = "Builds the minified jar"
    dependsOn(if (minifierMode == "gr8") gr8MinifiedJar else proguardedJar)
    val minJarFile: File = if (minifierMode == "gr8")
        tasks.named("gr8MinifiedShadowedJar").get()
            .outputs
            .files
            .filter { it.extension == "jar" }
            .singleFile
    else
        proguardedJar.get().outputs.files.singleFile

    inputs.files(minJarFile)
    outputs.files(layout.buildDirectory.file("libs/${minJarFile.name}").get().asFile)

    from(minJarFile.parentFile) {
        include(minJarFile.name)
    }
    into(layout.buildDirectory.dir("libs").get().asFile)
}

graalvmNative {
    toolchainDetection = false

    binaries {
        create("optimized") {
            fallback = false
            richOutput = true
            useArgFile = true

            buildArgs.addAll(javacFlags
                .plus(gvmFlags)
                .plus(entrypoint))

            buildArgs.addAll(project.provider {
                listOf(
                    "--module-path",
                    truffle.asPath,
                )
            })
            jvmArgs.addAll(jvmFlags)

            classpath(
                minifiedJar.get().outputs.files.filter {
                    it.extension == "jar"
                }
            )
        }
    }

    agent {
        defaultMode = "standard"
        builtinCallerFilter = true
        builtinHeuristicFilter = true
        trackReflectionMetadata = true
        enableExperimentalPredefinedClasses = true
        enableExperimentalUnsafeAllocationTracing = true
        enabled = properties.containsKey("agent")
        modes {
            standard {}
        }
        metadataCopy {
            inputTaskNames.addAll(listOf("run"))
            outputDirectories.add("src/main/resources/META-INF/native-image")
            mergeWithExisting = true
        }
    }
}

configurations.all {
    listOf(
        "com.github.ajalt.mordant:mordant-jvm-jna",
        "com.github.ajalt.mordant:mordant-jvm-ffm",
        // "org.apache.groovy:groovy-bom",
        "com.fasterxml.jackson:jackson-bom",
        "com.fasterxml.jackson.module:jackson-module-kotlin",
    ).forEach {
        val (grp, mod) = it.split(":")
        exclude(group = grp, module = mod)
    }

    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion(kotlinVersion)
            because("kotlin sdk pin")
        }
    }
}

tasks.test {
    jvmArgs(jvmFlags)
}

tasks.named("run", JavaExec::class) {
    jvmArgs(jvmFlags)
}

tasks.withType<JavaCompile>().configureEach {
    modularity.inferModulePath = true
    options.compilerArgumentProviders.add(CommandLineArgumentProvider {
        listOf(
            "--release=$jvmTarget",
        ).plus(
            javacOnlyFlags
        )
    })
}

tasks.withType<KotlinJvmCompile>().configureEach {
    jvmTargetValidationMode = JvmTargetValidationMode.WARNING
}

listOf(
    "nativeCompile",
    "nativeOptimizedCompile",
).forEach {
    tasks.named(it).configure { dependsOn(tasks.jar, tasks.shadowJar, minifiedJar, proguardedJar) }
}

tasks.withType<Jar>().configureEach {
    doLast {
        @Suppress("DEPRECATION") exec {
            commandLine("du", "-hac", outputs.files.singleFile.absolutePath)
        }
    }
}

tasks.withType<ProGuardTask>().configureEach {
    doLast {
        @Suppress("DEPRECATION") exec {
            commandLine("du", "-hac", "build/libs")
        }
    }
}

tasks.withType<BuildNativeImageTask>().configureEach {
    doLast {
        @Suppress("DEPRECATION") exec {
            commandLine("du", "-hac", "build/native/$name", "build/libs")
        }
    }
}

tasks.build {
    dependsOn(minifiedJar)

    doLast {
        @Suppress("DEPRECATION") exec {
            commandLine("du", "-hac", "build/libs")
        }
    }
}

listOf(
    "distTar",
    "distZip",
    "startScripts",
    "startShadowScripts",
).forEach {
    tasks.named(it).configure {
        dependsOn("minifiedJar")
    }
}
