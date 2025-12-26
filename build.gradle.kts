plugins {
    id("java")
    id("application")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "org.example"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.baulsupp.kolja:jcurses:0.9.5.3")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.20.1")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

application {
    mainClass.set("org.example.App")
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    archiveClassifier.set("") // делает shadow JAR основным
    manifest {
        attributes["Main-Class"] = "org.example.App"
    }
}
