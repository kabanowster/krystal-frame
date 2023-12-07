plugins {
    `java-library`
    `maven-publish`
    id("myJavaFx")
    id("coreTools")
    id("springCore")
}

group = "io.krystal"
version = "0.1"

dependencies {
    implementation(project(":tools"))
}