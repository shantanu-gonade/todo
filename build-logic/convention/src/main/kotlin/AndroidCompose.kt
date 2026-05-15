import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Wires Compose BOM and core Compose dependencies.
 * Compose feature flag (buildFeatures.compose = true) must be set in the calling plugin
 * using the concrete extension type (ApplicationExtension or LibraryExtension), because
 * AGP 9 removed type parameters from CommonExtension and buildFeatures is only accessible
 * on the concrete extension types.
 */
internal fun Project.configureAndroidComposeDependencies() {
    dependencies {
        val bom = libs.findLibrary("androidx-compose-bom").get()
        add("implementation", platform(bom))
        add("androidTestImplementation", platform(bom))
        add("implementation", libs.findLibrary("androidx-compose-ui").get())
        add("implementation", libs.findLibrary("androidx-compose-ui-graphics").get())
        add("implementation", libs.findLibrary("androidx-compose-ui-tooling-preview").get())
        add("implementation", libs.findLibrary("androidx-compose-material3").get())
        add("debugImplementation", libs.findLibrary("androidx-compose-ui-tooling").get())
    }
}
