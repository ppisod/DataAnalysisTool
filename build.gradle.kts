plugins {
    id("application")
    id("org.openjfx.javafxplugin") version "0.1.0"
}

javafx {
    version = "26"
    modules("javafx.controls", "javafx.fxml")
}

group = "org.jackl"
version = "0.1"

application {
    mainClass.set("org.jackl.Launcher")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.xerial:sqlite-jdbc:3.45.3.0")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
