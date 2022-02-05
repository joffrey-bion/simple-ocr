plugins {
    kotlin("jvm") version "1.6.10"
}

group = "org.hildan.ocr"
description = "A simple OCR that recognizes characters in an image given a set of base character images"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.2")
}

tasks.test {
    useJUnitPlatform()
}
