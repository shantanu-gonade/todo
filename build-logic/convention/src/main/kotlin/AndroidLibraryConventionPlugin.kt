import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")
        // NOTE: Do NOT apply org.jetbrains.kotlin.android here.
        // AGP 9.0+ has built-in Kotlin support enabled by default.
        // Applying kotlin-android would cause "extension 'kotlin' already registered".
        extensions.configure<LibraryExtension> {
            compileSdk = COMPILE_SDK
            defaultConfig {
                minSdk = MIN_SDK
            }
            compileOptions {
                sourceCompatibility = JAVA_VERSION
                targetCompatibility = JAVA_VERSION
            }
        }
    }
}
