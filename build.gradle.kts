plugins {
    `java-library`
    `maven-publish`
    id("myJavaFx")
    id("coreTools")
    id("springCore")
    id("springWebflux")
}

group = "io.krystal"
version = "0.1"

dependencies {
    api(project(":tools"))
}