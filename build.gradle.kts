import java.net.URI

plugins {
    `java-library`
    `maven-publish`
    id("net.linguica.maven-settings") version "0.5"
    id("myJavaFx")
    id("coreTools")
    id("springCore")
    id("springWebflux")
}

group = "io.krystal"
version = "1.0.2"

java {
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    api(project(":tools"))
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
                description = "Java framework based on Spring Core and Webflux modules, JavaFX, Tomcat, Log4j2 and various JDBC connectors."
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
    repositories {
        maven {
            name = "BDE-Development"
            url = URI("https://dgd365o.pkgs.visualstudio.com/aa703476-5cc4-43ae-82af-8acbab9dab87/_packaging/BDE-Development/maven/v1")
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}

tasks.javadoc {
    if (JavaVersion.current().isJava9Compatible) {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
}