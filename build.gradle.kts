plugins {
    `java-library`
    id("io.krystal.setup")
    id("io.krystal.publish")
    id("io.krystal.myJavaFx")
}

group = "io.krystal"
version = "1.23.10"

java {
    withSourcesJar()
}

tasks.javadoc {
    destinationDir = file("$projectDir/javadoc")

    source = fileTree("src/main/java") {
        include("krystal/**")
    }

    classpath = sourceSets.main.get().compileClasspath

    options {
        val opts = this as StandardJavadocDocletOptions
        opts.addStringOption("sourcepath", file("src/main/java").absolutePath)
        opts.addStringOption("Xdoclint:none", "-quiet")
        opts.memberLevel = JavadocMemberLevel.PACKAGE
        opts.encoding = "UTF-8"
        opts.links(
            "https://docs.oracle.com/en/java/javase/17/docs/api/",
            "https://docs.spring.io/spring-framework/docs/current/javadoc-api/"
        )
    }

    isFailOnError = false
}

dependencies {
    api(project(":tools"))
    jdbc(Config.api)
    http(Config.api)
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