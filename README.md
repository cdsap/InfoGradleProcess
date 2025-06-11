# Info Gradle Process Plugin
Includes information about Gradle processes in the Build Scans or in the build output.
The plugin is compatible with configuration cache.

## Usage
Apply the plugin in the main `build.gradle(.kts)` configuration file:

#### Kotlin
Using the plugins DSL:
``` groovy
plugins {
  id("io.github.cdsap.gradleprocess") version "0.2.0"
}
```

Using legacy plugin application:
``` groovy
buildscript {
  repositories {
    gradlePluginPortal()
  }
  dependencies {
    classpath("io.github.cdsap:infogradleprocess:0.2.0")
  }
}

apply(plugin = "io.github.cdsap.gradleprocess")
```

#### Groovy
Using the plugins DSL:
``` groovy
plugins {
  id "io.github.cdsap.gradleprocess" version "0.2.0"
}

```

Using legacy plugin application:
``` groovy
buildscript {
  repositories {
    gradlePluginPortal()
  }
  dependencies {
    classpath "io.github.cdsap:infogradleprocess:0.2.0"
  }
}

apply plugin: "io.github.cdsap.gradleprocess"
```
## Output
### Build Scans
If you are using Develocity, the information about the Gradle processes will be included as custom value in the
Build Scan:

![](images/buildscan.png)

The field `Usage` represents the value obtained at the end of the build using `jstat` on the JVM process.

> [!NOTE]
Develocity 2024.2 provides new resource usage endpoints with detailed information about the different build and child processes during the execution:
https://docs.gradle.com/develocity/api-manual/ref/2024.2.html#tag/Builds/operation/GetGradleResourceUsage

### Build Output
If you are not using Develocity, the information about the Gradle processes will be included at the end of the build:
```
> Task :core:ui:compileProdDebugKotlin
┌─────────────────────────────────────────────────────────────────────────────┐
│  Gradle processes                                                           │
├─────────┬──────────┬───────────┬────────────┬───────────────┬───────────────┤
│  PID    │  Max     │  Usage    │  Capacity  │  GC Time      │  Uptime       │
├─────────┼──────────┼───────────┼────────────┼───────────────┼───────────────┤
│  10865  │  1.0 Gb  │  0.66 Gb  │  1.0 Gb    │  0.0 minutes  │  0.0 minutes  │
├─────────┼──────────┼───────────┼────────────┼───────────────┼───────────────┤
│  9011   │  0.5 Gb  │  0.2 Gb   │  0.5 Gb    │  0.0 minutes  │  0.0 minutes  │
└─────────┴──────────┴───────────┴────────────┴───────────────┴───────────────┘
BUILD SUCCESSFUL in 35s

```

## Requirements
* Gradle 7.5+

## Libraries
* `com.gradle:develocity-gradle-plugin`
* `com.jakewharton.picnic:picnic`
