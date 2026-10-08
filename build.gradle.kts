import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    `maven-publish`
}

group = "org.crosswire"
version = "2.3-SNAPSHOT"

kotlin {
    jvmToolchain(17)

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
