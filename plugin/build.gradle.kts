import org.gradle.plugin.compatibility.compatibility

plugins {
    `java-gradle-plugin`
    `maven-publish`
    `kotlin-dsl`
    alias(libs.plugins.pluginPublish)
}

group = "io.github.cdsap"
version = "0.3.1-SNAPSHOT"

// Preserve pre-split Maven/JAR coordinates (previously the root project name).
base {
    archivesName.set("InfoGradleProcess")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation(libs.cdsap.jdkToolsParser)
    implementation(libs.cdsap.commandlineValueSource)
    implementation(libs.picnic)
    compileOnly(libs.develocity.gradlePlugin)
    // On the test JVM so ProjectBuilder tests see DevelocityConfiguration without applying it.
    testImplementation(libs.develocity.gradlePlugin)
    testImplementation(libs.junit)
}
tasks.withType<Test>().configureEach {
    filter {

        if (project.hasProperty("excludeTests")) {
            excludeTest(project.property("excludeTests").toString(),"")
        }
    }
}
gradlePlugin {
    website.set("https://github.com/cdsap/InfoGradleProcess")
    vcsUrl.set("https://github.com/cdsap/InfoGradleProcess")
    plugins {
        create("InfoGradleProcessPlugin") {
            id = "io.github.cdsap.gradleprocess"
            displayName = "Info Gradle Processes"
            description = "Retrieve information of the Gradle processes after the build execution"
            implementationClass = "io.github.cdsap.gradleprocess.InfoGradleProcessPlugin"
            tags.set(listOf("process"))
            compatibility {
                features {
                    configurationCache = true
                }
            }
        }
        create("InfoGradleProcessProjectPlugin") {
            id = "io.github.cdsap.gradleprocess.project"
            displayName = "Info Gradle Processes (project)"
            description =
                "Project-only alias of io.github.cdsap.gradleprocess; prefer applying io.github.cdsap.gradleprocess in settings"
            implementationClass = "io.github.cdsap.gradleprocess.InfoGradleProcessProjectPlugin"
            tags.set(listOf("process"))
            compatibility {
                features {
                    configurationCache = true
                }
            }
        }
    }
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
    publications.withType<MavenPublication>().configureEach {
        // java-gradle-plugin's pluginMaven defaults to the project name ("plugin");
        // keep the historical artifact id from when sources lived in the root project.
        if (name == "pluginMaven") {
            artifactId = "InfoGradleProcess"
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
