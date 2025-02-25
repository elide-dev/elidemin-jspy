
pluginManagement {
    repositories {
        maven {
            name = "elide-snapshots"
            url = uri("https://maven.elide.dev")
            content {
                includeGroup("dev.elide")
                includeGroup("dev.elide.tools")
                includeGroup("com.google.devtools.ksp")
                includeGroup("org.jetbrains.reflekt")
                includeGroup("org.pkl-lang")
            }
        }
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots/") {
            mavenContent { snapshotsOnly() }
        }
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        maven {
            name = "elide-snapshots"
            url = uri("https://maven.elide.dev")
            content {
                includeGroup("dev.elide")
                includeGroup("dev.elide.tools")
                includeGroup("com.google.devtools.ksp")
                includeGroup("org.jetbrains.reflekt")
                includeGroup("org.pkl-lang")
            }
        }
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots/") {
            mavenContent { snapshotsOnly() }
        }
        mavenCentral()
        google()
    }
}

rootProject.name="labs"
