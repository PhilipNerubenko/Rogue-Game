plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    flatDir {
        dirs("libs")  // Без точки с запятой, в скобках
    }
}

dependencies {
    // https://jarcasting.com/artifacts/com.baulsupp.kolja/jcurses/0.9.5.3/
    implementation(files("libs/jcurses-0.9.5.3.jar"))  // файл jcurses.jar в папке libs/

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}