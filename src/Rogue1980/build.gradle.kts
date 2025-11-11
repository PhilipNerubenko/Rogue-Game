plugins {
    id("java")
    id("application")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    flatDir {
        dirs("libs")
    }
}

dependencies {
    implementation(files("libs/jcurses-0.9.5.3.jar"))

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Укажите главный класс с main() вашей программы
application {
    mainClass.set("org.example.Main")
    applicationDefaultJvmArgs = listOf("-Djava.library.path=${projectDir}/libs")
}

// Обеспечим, что JVM параметр java.library.path передается в другие задачи запуска
tasks.withType<JavaExec> {
    jvmArgs = listOf("-Djava.library.path=${projectDir}/libs")
}

tasks.test {
    useJUnitPlatform()
}