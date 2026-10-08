import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    `maven-publish`
}

group = "org.crosswire"
version = "2.3-SNAPSHOT"

kotlin {
    jvmToolchain(17)

    // @JvmStatic/@JvmField etc. are optional expectations in common code; converted files need no file-level opt-in.
    compilerOptions { optIn.add("kotlin.ExperimentalMultiplatform") }

    // JVM target: hosts the not-yet-converted Java (src/jvmMain/java) next to converted Kotlin.
    jvm {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }
    // iOS targets: compile-checked on Linux; commonMain only.
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        jvmMain.dependencies {
            implementation("org.apache.commons:commons-compress:1.12")
            implementation("com.chenlb.mmseg4j:mmseg4j-analysis:1.8.6")
            implementation("com.chenlb.mmseg4j:mmseg4j-dic:1.8.6")
            implementation("org.jdom:jdom2:2.0.6.1")
            implementation("org.apache.lucene:lucene-analyzers:3.6.2")
            implementation("org.slf4j:slf4j-api:1.7.6")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        jvmTest.dependencies {
            implementation("junit:junit:4.13")
            implementation(kotlin("test-junit"))
            implementation("org.slf4j:slf4j-simple:1.7.6")
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.named<Test>("jvmTest") {
    useJUnit()
    systemProperty("user.language", "en")
    systemProperty("user.country", "US")
    exclude("org/crosswire/ksword/parity/**")
}

// --- KSword parity oracle (K0 spec §6) ---------------------------------------------------------------
// The baseline's own third-party libraries, pinned to the baseline versions; only the oracle loader sees them.
val oracleThirdParty by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}
dependencies {
    oracleThirdParty("org.apache.commons:commons-compress:1.12")
    oracleThirdParty("com.chenlb.mmseg4j:mmseg4j-analysis:1.8.6")
    oracleThirdParty("com.chenlb.mmseg4j:mmseg4j-dic:1.8.6")
    oracleThirdParty("org.jdom:jdom2:2.0.6.1")
    oracleThirdParty("org.apache.lucene:lucene-analyzers:3.6.2")
    oracleThirdParty("org.slf4j:slf4j-api:1.7.6")
    oracleThirdParty("org.slf4j:slf4j-simple:1.7.6")
}

val baselineJar = layout.projectDirectory.file("oracle/jsword-baseline.jar")

// ParityProbe compiled against the baseline jar, for the oracle class loader.
val compileOracleProbe by tasks.registering(JavaCompile::class) {
    source(layout.projectDirectory.dir("src/jvmTest/java/org/crosswire/ksword/parity"))
    include("ParityProbe.java")
    classpath = files(baselineJar) + oracleThirdParty
    destinationDirectory.set(layout.buildDirectory.dir("oracleProbe/classes"))
    options.encoding = "UTF-8"
    options.release.set(17)
    sourceCompatibility = "17"
    targetCompatibility = "17"
    javaCompiler.set(javaToolchains.compilerFor { languageVersion.set(JavaLanguageVersion.of(17)) })
}

val jvmTestCompilation = kotlin.jvm().compilations.getByName("test")

val parityTest by tasks.registering(Test::class) {
    group = "verification"
    description = "Compares the current code with the frozen KSword baseline (needs ~/.sword test modules)."
    dependsOn(compileOracleProbe, tasks.named("jvmTestClasses"))
    testClassesDirs = jvmTestCompilation.output.classesDirs
    classpath = files(jvmTestCompilation.output.allOutputs, jvmTestCompilation.runtimeDependencyFiles)
    useJUnit()
    filter { includeTestsMatching("org.crosswire.ksword.parity.*") }
    systemProperty("user.language", "en")
    systemProperty("user.country", "US")
    systemProperty("ksword.projectDir", layout.projectDirectory.asFile.absolutePath)
    val oracleClasspath = files(baselineJar, compileOracleProbe.map { it.destinationDirectory }) + oracleThirdParty
    inputs.files(oracleClasspath)
    jvmArgumentProviders.add(CommandLineArgumentProvider {
        listOf("-Dksword.oracle.classpath=${oracleClasspath.asPath}")
    })
}
