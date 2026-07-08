plugins {
    `java-library`
    `maven-publish`
//    id("org.jetbrains.kotlin.jvm") version '1.4.10'
}


java {
    // Emit Java 17 bytecode regardless of the JDK running Gradle, so :jsword classes
    // load in the :app module's Java 17 test/runtime toolchain. (CI runs on JDK 17;
    // the in-container JAVA_HOME is JDK 21, which would otherwise produce Java 21
    // bytecode the Java 17 test JVM cannot load.)
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}

tasks.withType<Test>() {
    // Enforce en-US locale, as many unit tests are locale-dependant.
    systemProperty("user.language", "en")
    systemProperty("user.country", "US")
}

group = "org.crosswire"
version = "2.3-SNAPSHOT"

dependencies {
//    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.apache.commons:commons-compress:1.12")
    implementation("com.chenlb.mmseg4j:mmseg4j-analysis:1.8.6")
    implementation("com.chenlb.mmseg4j:mmseg4j-dic:1.8.6")

    implementation("org.jdom:jdom2:2.0.6.1")
    implementation("org.apache.lucene:lucene-analyzers:3.6.2")
    // To upgrade Lucene, change to
    // implementation("org.apache.lucene:lucene-analyzers-common:x")

    //implementation("org.slf4j:slf4j-api:1.7.6")
    implementation("org.slf4j:slf4j-api:1.7.6")
    testImplementation("org.slf4j:slf4j-simple:1.7.6")
    testImplementation("junit:junit:4.13")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            }
        }
    }

