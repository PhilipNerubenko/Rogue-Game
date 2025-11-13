plugins {
    id("java")
    id("application")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    flatDir { dirs("libs") }
}

dependencies {
    implementation(files("libs/jcurses.jar"))

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

application {
    mainClass.set("org.example.Main")
}

// ⬇️⬇️⬇️ ВОТ ЭТОТ БЛОК РЕШАЕТ ВСЕ ПРОБЛЕМЫ ⬇️⬇️⬇️
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
            "Main-Class" to "org.example.Main",
            "Class-Path" to "libs/jcurses.jar"
        )
    }
}
// ⬆️⬆️⬆️ ДОБАВЬТЕ ВОТ ЭТО ⬆️⬆️⬆️

// Задача для удобного запуска
tasks.register("runGame") {
    group = "application"
    description = "Запуск Rogue1980"

    dependsOn("jar")

    doFirst {
        javaexec {
            workingDir = projectDir
            mainClass.set("org.example.Main")
            classpath = sourceSets.main.get().runtimeClasspath + files("libs")
            systemProperty("java.library.path", "libs")
            standardInput = System.`in`
        }
    }
}