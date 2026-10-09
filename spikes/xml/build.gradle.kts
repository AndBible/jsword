plugins { id("org.jetbrains.kotlin.multiplatform") }

kotlin {
    jvmToolchain(17)
    jvm()
    iosArm64()
    iosSimulatorArm64()
    sourceSets {
        commonMain.dependencies {
            implementation("com.fleeksoft.ksoup:ksoup:0.2.6")
            implementation("io.github.pdvrieze.xmlutil:core:0.91.3")
        }
        jvmTest.dependencies {
            implementation(project(":jsword"))
            implementation("org.jdom:jdom2:2.0.6.1")
            implementation("jaxen:jaxen:1.2.0")
            implementation(kotlin("test-junit"))
        }
    }
}

tasks.named<Test>("jvmTest") { useJUnit(); maxHeapSize = "2g" }
