import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

// Version catalog accessor used by convention plugins.
internal val Project.libs
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

// Shared SDK/Java constants — used directly in each plugin's configure block.
internal val JAVA_VERSION = JavaVersion.VERSION_17
internal const val COMPILE_SDK = 36
internal const val MIN_SDK = 28
internal const val TARGET_SDK = 36

// NOTE: configureKotlinJvm() removed — AGP 9.0 built-in Kotlin handles jvmTarget
// automatically, defaulting to android.compileOptions.targetCompatibility.
