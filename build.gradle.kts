// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.detekt) apply false
}

// Root-level Spotless configuration — applies formatting rules to all subprojects.
// Individual modules opt in via the SpotlessConventionPlugin in build-logic.
subprojects {
    apply(plugin = "com.diffplug.spotless")
}

// Aggregate detekt task so `./gradlew detekt` works from the root (mirrors CI).
// Uses gradle.projectsEvaluated so all subproject tasks are registered before
// we wire up dependencies — compatible with Gradle 9 strict project isolation.
val detektAll = tasks.register("detekt") {
    group = "verification"
    description = "Runs Detekt static analysis across all subprojects."
}

gradle.projectsEvaluated {
    subprojects.forEach { sub ->
        sub.tasks.findByName("detekt")?.let { subDetekt ->
            detektAll.configure { dependsOn(subDetekt) }
        }
    }
}
