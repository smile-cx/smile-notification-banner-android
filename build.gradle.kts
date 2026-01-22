// Top-level build file

// Library version - update this for new releases
val libraryVersion = "2.5.1"

// Make version available to subprojects
extra["libraryVersion"] = libraryVersion

plugins {
    id("com.android.application") version "8.13.2" apply false
    id("com.android.library") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
    id("com.vanniktech.maven.publish") version "0.25.3" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
