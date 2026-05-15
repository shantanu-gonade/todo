import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")
        // NOTE: Do NOT apply org.jetbrains.kotlin.android here.
        // AGP 9.0+ has built-in Kotlin support enabled by default.
        // Applying kotlin-android would cause "extension 'kotlin' already registered".
        extensions.configure<ApplicationExtension> {
            compileSdk = COMPILE_SDK
            defaultConfig {
                minSdk = MIN_SDK
                targetSdk = TARGET_SDK
            }
            compileOptions {
                sourceCompatibility = JAVA_VERSION
                targetCompatibility = JAVA_VERSION
                // With AGP 9 built-in Kotlin, jvmTarget defaults to targetCompatibility.
            }
        }
    }
}
