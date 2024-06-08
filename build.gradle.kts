plugins {
    `java-library`
    id("setup")
    id("publish")
    id("myJavaFx")
}

group = "io.krystal"
version = "1.7.0"

java {
    //withJavadocJar()
    withSourcesJar()
}

dependencies {
    // impl
    jdbc()
    processing()

    // api
    api(project(":tools"))
    jfxVisuals(Config.api)
    coreTools(Config.api)
    springCore(Config.api)
    tomcatServer(Config.api)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "krystal-frame"
            from(components["java"])
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
            pom {
                name = "Krystal Frame"
                description = "Java framework based on Spring Core modules, JavaFX, Tomcat, Log4j2 and various JDBC connectors."
                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }
                developers {
                    developer {
                        id = "kabanowster"
                        name = "Wiktor Kabanow"
                        email = "kabanowster@gmail.com"
                    }
                }
            }
        }
    }
}