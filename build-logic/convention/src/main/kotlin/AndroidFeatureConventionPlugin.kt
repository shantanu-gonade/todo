import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("todoapp.android.library")
            apply("todoapp.android.library.compose")
            apply("todoapp.android.hilt")
            // Type-safe Navigation Compose routes (composable<RouteKey>) require the
            // kotlinx serialization COMPILER plugin to generate serializers at build time.
            // Without this, @Serializable classes on route keys throw SerializationException
            // at runtime even though the annotation is present.
            apply("org.jetbrains.kotlin.plugin.serialization")
        }
        dependencies {
            add("implementation", project(":core:ui"))
            add("implementation", project(":core:designsystem"))
            add("implementation", project(":core:domain"))
            add("implementation", project(":core:model"))
            add("implementation", project(":core:common"))
            add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())
            add("implementation", libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
            add("implementation", libs.findLibrary("hilt-navigation-compose").get())
            add("implementation", libs.findLibrary("androidx-navigation-compose").get())
            // Required at runtime for Navigation Compose type-safe routes.
            // The compiler plugin generates the serializers; this library provides
            // the runtime that Navigation Compose calls when building the back stack.
            add("implementation", libs.findLibrary("kotlinx-serialization-json").get())
            add("testImplementation", libs.findLibrary("junit").get())
            add("testImplementation", libs.findLibrary("turbine").get())
            add("testImplementation", libs.findLibrary("kotlinx-coroutines-test").get())
        }
    }
}
