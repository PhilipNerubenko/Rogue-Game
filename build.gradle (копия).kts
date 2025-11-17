plugins {
    id("java")
    id("application")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    //flatDir { dirs("libs") }
}

dependencies {
    //implementation(files("libs/jcurses.jar"))
    implementation("com.baulsupp.kolja:jcurses:0.9.5.3")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

application {
    mainClass.set("org.example.App")
}

tasks.jar {
    // ВКЛЮЧАЕМ скомпилированные классы в JAR
    from(sourceSets.main.get().output)

    // ВКЛЮЧАЕМ зависимости (jcurses.jar) в JAR
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.contains("jcurses") }
            .map { zipTree(it) }
    })

    // НАСТРАИВАЕМ манифест
    manifest {
        attributes(
            "Main-Class" to "org.example.App",
            "Class-Path" to "libs/jcurses.jar"
        )
    }
}

