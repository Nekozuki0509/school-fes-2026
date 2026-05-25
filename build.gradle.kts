plugins {
    java
    application
    id("org.javamodularity.moduleplugin") version "1.8.15"
    id("org.openjfx.javafxplugin") version "0.0.13"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.beryx.jlink") version "2.25.0"
}

group = "com.github.nekozuki0509"
version = "1.0.0"

repositories {
    mavenCentral()
}

val junitVersion = "5.12.1"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(23)
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

application {
    mainModule.set("com.github.nekozuki0509.schoolfes2026")
    mainClass.set("com.github.nekozuki0509.schoolfes2026.Launcher")
}

javafx {
    version = "25"
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.media")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:${junitVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${junitVersion}")

    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")
    testCompileOnly("org.projectlombok:lombok:1.18.38")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.38")

    implementation("com.google.code.gson:gson:2.13.2")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

jlink {
    imageZip.set(layout.buildDirectory.file("/distributions/app-${javafx.platform.classifier}.zip"))
    options.set(listOf("--strip-debug", "--compress=2", "--no-header-files", "--no-man-pages"))
    launcher {
        name = "app"
    }
    jpackage {
        installerType = "app-image"
        imageOptions = listOf("--win-console")
    }
}

val jar by tasks.getting(Jar::class) {
    project.setProperty("mainClassName", "com.github.nekozuki0509.schoolfes2026.Launcher")
    manifest {
        attributes["Main-Class"] = "com.github.nekozuki0509.schoolfes2026.Launcher"
    }
}
