plugins {
    `java-gradle-plugin`
    `maven-publish`
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "1.3.1"
}

group = "io.github.cdsap"
version = "0.3.1-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation("io.github.cdsap:jdk-tools-parser:0.1.1")
    implementation("io.github.cdsap:commandline-value-source:0.1.0")
    implementation("com.jakewharton.picnic:picnic:0.7.0")
    compileOnly("com.gradle:develocity-gradle-plugin:4.2.2")
    testImplementation("junit:junit:4.13.2")
}
tasks.withType<Test>().configureEach {
    filter {

        if (project.hasProperty("excludeTests")) {
            excludeTest(project.property("excludeTests").toString(),"")
        }
    }
}
gradlePlugin {
    plugins {
        create("InfoGradleProcessPlugin") {
            id = "io.github.cdsap.gradleprocess"
            displayName = "Info Gradle Processes"
            description = "Retrieve information of the Gradle processes after the build execution"
            implementationClass = "io.github.cdsap.gradleprocess.InfoGradleProcessPlugin"
        }
    }
}
pluginBundle {
    website = "https://github.com/cdsap/InfoGradleProcess"
    vcsUrl = "https://github.com/cdsap/InfoGradleProcess"
    tags = listOf("process")
}

publishing {
    repositories {
        maven {
            name = "Snapshots"
            url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")

            credentials {
                username = System.getenv("USERNAME_SNAPSHOT")
                password = System.getenv("PASSWORD_SNAPSHOT")
            }
        }
        maven {
            name = "Release"
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")

            credentials {
                username = System.getenv("USERNAME_SNAPSHOT")
                password = System.getenv("PASSWORD_SNAPSHOT")
            }
        }
    }
    publications {
        create<MavenPublication>("gradleProcessPublication") {
            from(components["java"])
            artifactId = "gradleprocess"
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
            pom {
                scm {
                    connection.set("scm:git:git://github.com/cdsap/InfoGradleProcess/")
                    url.set("https://github.com/cdsap/InfoGradleProcess/")
                }
                name.set("InfoGradleProcess")
                url.set("https://github.com/cdsap/InfoGradleProcess/")
                description.set(
                    "Retrieve information of the Gradle process in your Build Scan or console"
                )
                licenses {
                    license {
                        name.set("The MIT License (MIT)")
                        url.set("https://opensource.org/licenses/MIT")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("cdsap")
                        name.set("Inaki Villar")
                    }
                }
            }
        }
    }
}
