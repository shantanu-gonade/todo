# Phase 0 — Build Foundation

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans.
> Read `01-architecture-and-design.md` before starting.

**Produces:** a compiling empty multi-module skeleton — no app behavior yet.

**Starting point:** the scaffold at `Eulerity/todo/` has Gradle 9.1, AGP 9.0.1,
`rootProject.name = "todo"`, and `:app` with namespace/applicationId
`com.eulerity.todo` and `minSdk 24` (this phase corrects it to 28). All Gradle
commands run from `Eulerity/todo/`.

---

## Task 0.1: Expand the version catalog

**Files:**
- Modify: `gradle/libs.versions.toml`

**Step 1: Replace the catalog with the full dependency set**

Replace the entire contents of `gradle/libs.versions.toml` with:

```toml
[versions]
agp = "9.0.1"
kotlin = "2.1.0"
ksp = "2.1.0-1.0.29"
coreKtx = "1.15.0"
lifecycle = "2.8.7"
activityCompose = "1.9.3"
composeBom = "2026.05.00"
navigationCompose = "2.8.5"
hilt = "2.53"
hiltNavigationCompose = "1.2.0"
room = "2.6.1"
datastore = "1.1.1"
coroutines = "1.9.0"
kotlinxDatetime = "0.6.1"
kotlinxSerialization = "1.7.3"
workManager = "2.10.0"
junit = "4.13.2"
turbine = "1.2.0"
robolectric = "4.14"
androidxTestExt = "1.2.1"
androidxTestCore = "1.6.1"
espresso = "3.6.1"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycle" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltNavigationCompose" }
hilt-work = { group = "androidx.hilt", name = "hilt-work", version.ref = "hiltNavigationCompose" }
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }
datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
kotlinx-coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
kotlinx-datetime = { group = "org.jetbrains.kotlinx", name = "kotlinx-datetime", version.ref = "kotlinxDatetime" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerialization" }
work-runtime-ktx = { group = "androidx.work", name = "work-runtime-ktx", version.ref = "workManager" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }
robolectric = { group = "org.robolectric", name = "robolectric", version.ref = "robolectric" }
androidx-test-ext-junit = { group = "androidx.test.ext", name = "junit", version.ref = "androidxTestExt" }
androidx-test-core = { group = "androidx.test", name = "core", version.ref = "androidxTestCore" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espresso" }

# Plugin artifacts referenced by convention plugins
android-gradlePlugin = { group = "com.android.tools.build", name = "gradle", version.ref = "agp" }
kotlin-gradlePlugin = { group = "org.jetbrains.kotlin", name = "kotlin-gradle-plugin", version.ref = "kotlin" }
ksp-gradlePlugin = { group = "com.google.devtools.ksp", name = "com.google.devtools.ksp.gradle.plugin", version.ref = "ksp" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
room = { id = "androidx.room", version.ref = "room" }

# Convention plugins defined in build-logic (registered in Task 0.3)
todoapp-android-application = { id = "todoapp.android.application" }
todoapp-android-application-compose = { id = "todoapp.android.application.compose" }
todoapp-android-library = { id = "todoapp.android.library" }
todoapp-android-library-compose = { id = "todoapp.android.library.compose" }
todoapp-android-feature = { id = "todoapp.android.feature" }
todoapp-android-hilt = { id = "todoapp.android.hilt" }
todoapp-android-room = { id = "todoapp.android.room" }
todoapp-jvm-library = { id = "todoapp.jvm.library" }
todoapp-android-test = { id = "todoapp.android.test" }
```

**Step 2: Verify the catalog parses**

Run: `./gradlew help`
Expected: `BUILD SUCCESSFUL`. If a version is rejected as not found, adjust to
the nearest published version — the catalog is the single place to fix versions.

**Step 3: Commit**

```bash
git add gradle/libs.versions.toml
git commit -m "build: expand version catalog with full dependency set"
```

## Task 0.2: Register the build-logic included build

**Files:**
- Modify: `settings.gradle.kts`
- Create: `build-logic/settings.gradle.kts`
- Create: `build-logic/convention/build.gradle.kts`

**Step 1: Update `settings.gradle.kts`**

Add `includeBuild("build-logic")` as the first line inside `pluginManagement { }`,
and replace the module includes at the bottom:

```kotlin
pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "todo"
include(":app")
include(":core:model")
include(":core:common")
include(":core:database")
include(":core:datastore")
include(":core:data")
include(":core:domain")
include(":core:designsystem")
include(":core:ui")
include(":feature:today")
include(":feature:history")
```

**Step 2: Create `build-logic/settings.gradle.kts`**

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
include(":convention")
```

**Step 3: Create `build-logic/convention/build.gradle.kts`**

```kotlin
plugins {
    `kotlin-dsl`
}

group = "com.eulerity.todo.buildlogic"

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
}
```

**Step 4: Verify Gradle still configures**

Run: `./gradlew projects`
Expected: `BUILD SUCCESSFUL` listing `:app` and the `:core:*` / `:feature:*`
modules. They will not build until their `build.gradle.kts` files exist — that is
fine; `projects` only configures settings.

**Step 5: Commit**

```bash
git add settings.gradle.kts build-logic
git commit -m "build: register build-logic included build and module includes"
```

## Task 0.3: Write the convention plugins

**Files:**
- Create nine plugin files plus two shared helpers under
  `build-logic/convention/src/main/kotlin/`

Long but mechanical. Each plugin is a `Plugin<Project>` that applies other
plugins and configures common options.

**Step 1: Shared Kotlin/Android helper — `KotlinAndroid.kt`**

```kotlin
import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

internal val Project.libs
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    commonExtension.apply {
        compileSdk = 36
        defaultConfig {
            minSdk = 28
        }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            allWarningsAsErrors.set(false)
        }
    }
}
```

> `minSdk` is set to **28** here — this corrects the scaffold's `minSdk 24` and
> satisfies the exercise requirement in one central place.

**Step 2: `AndroidApplicationConventionPlugin.kt`**

```kotlin
import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("com.android.application")
            apply("org.jetbrains.kotlin.android")
        }
        extensions.configure<ApplicationExtension> {
            configureKotlinAndroid(this)
            defaultConfig.targetSdk = 36
        }
    }
}
```

**Step 3: `AndroidLibraryConventionPlugin.kt`**

```kotlin
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("com.android.library")
            apply("org.jetbrains.kotlin.android")
        }
        extensions.configure<LibraryExtension> {
            configureKotlinAndroid(this)
        }
    }
}
```

**Step 4: Compose helper + two compose plugins**

`AndroidCompose.kt`:
```kotlin
import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

internal fun Project.configureAndroidCompose(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    commonExtension.apply {
        buildFeatures { compose = true }
    }
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
```

`AndroidApplicationComposeConventionPlugin.kt`:
```kotlin
import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidApplicationComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        val extension = extensions.getByType(ApplicationExtension::class.java)
        configureAndroidCompose(extension)
    }
}
```

`AndroidLibraryComposeConventionPlugin.kt`:
```kotlin
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidLibraryComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        val extension = extensions.getByType(LibraryExtension::class.java)
        configureAndroidCompose(extension)
    }
}
```

**Step 5: `AndroidHiltConventionPlugin.kt`**

```kotlin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidHiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("com.google.devtools.ksp")
            apply("com.google.dagger.hilt.android")
        }
        dependencies {
            add("implementation", libs.findLibrary("hilt-android").get())
            add("ksp", libs.findLibrary("hilt-compiler").get())
        }
    }
}
```

**Step 6: `AndroidRoomConventionPlugin.kt`**

```kotlin
import androidx.room.gradle.RoomExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("androidx.room")
            apply("com.google.devtools.ksp")
        }
        extensions.configure<RoomExtension> {
            schemaDirectory("$projectDir/schemas")
        }
        dependencies {
            add("implementation", libs.findLibrary("room-runtime").get())
            add("implementation", libs.findLibrary("room-ktx").get())
            add("ksp", libs.findLibrary("room-compiler").get())
            add("testImplementation", libs.findLibrary("room-testing").get())
        }
    }
}
```

**Step 7: `JvmLibraryConventionPlugin.kt`**

```kotlin
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure

class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")
        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }
}
```

**Step 8: `AndroidFeatureConventionPlugin.kt`**

```kotlin
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
            add("testImplementation", libs.findLibrary("junit").get())
            add("testImplementation", libs.findLibrary("turbine").get())
            add("testImplementation", libs.findLibrary("kotlinx-coroutines-test").get())
        }
    }
}
```

**Step 9: `AndroidTestConventionPlugin.kt`**

```kotlin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        dependencies {
            add("testImplementation", libs.findLibrary("junit").get())
            add("testImplementation", libs.findLibrary("turbine").get())
            add("testImplementation", libs.findLibrary("robolectric").get())
            add("testImplementation", libs.findLibrary("kotlinx-coroutines-test").get())
            add("testImplementation", libs.findLibrary("androidx-test-core").get())
            add("testImplementation", libs.findLibrary("androidx-test-ext-junit").get())
        }
    }
}
```

**Step 10: Register all plugins** — append to
`build-logic/convention/build.gradle.kts`:

```kotlin
gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "todoapp.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidApplicationCompose") {
            id = "todoapp.android.application.compose"
            implementationClass = "AndroidApplicationComposeConventionPlugin"
        }
        register("androidLibrary") {
            id = "todoapp.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = "todoapp.android.library.compose"
            implementationClass = "AndroidLibraryComposeConventionPlugin"
        }
        register("androidFeature") {
            id = "todoapp.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("androidHilt") {
            id = "todoapp.android.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        register("androidRoom") {
            id = "todoapp.android.room"
            implementationClass = "AndroidRoomConventionPlugin"
        }
        register("jvmLibrary") {
            id = "todoapp.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
        register("androidTest") {
            id = "todoapp.android.test"
            implementationClass = "AndroidTestConventionPlugin"
        }
    }
}
```

**Step 11: Verify build-logic compiles**

Run: `./gradlew :build-logic:convention:compileKotlin`
Expected: `BUILD SUCCESSFUL`. If a plugin DSL import fails, the AGP version's API
surface may differ — check `com.android.build.api.dsl` for the current extension
types.

**Step 12: Commit**

```bash
git add build-logic
git commit -m "build: add nine convention plugins for shared module config"
```

## Task 0.4: Create module build files

**Files:** one `build.gradle.kts` per module (`:app` already exists, gets
replaced).

**Step 1: Replace `app/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.todoapp.android.application)
    alias(libs.plugins.todoapp.android.application.compose)
    alias(libs.plugins.todoapp.android.hilt)
}

android {
    namespace = "com.eulerity.todo"
    defaultConfig {
        applicationId = "com.eulerity.todo"
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":core:data"))
    implementation(project(":core:common"))
    implementation(project(":feature:today"))
    implementation(project(":feature:history"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.work.runtime.ktx)
    implementation(libs.hilt.work)
}
```

**Step 2: Create each core/feature module's build file** with exactly this
content. The directory for each module is the module path (e.g.
`core/model/build.gradle.kts`).

`core/model/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.todoapp.jvm.library)
    alias(libs.plugins.kotlin.serialization)
}
dependencies {
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}
```

`core/common/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.todoapp.android.library)
    alias(libs.plugins.todoapp.android.hilt)
}
android { namespace = "com.eulerity.todo.core.common" }
dependencies {
    implementation(project(":core:model"))
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
}
```

`core/database/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.todoapp.android.library)
    alias(libs.plugins.todoapp.android.hilt)
    alias(libs.plugins.todoapp.android.room)
}
android { namespace = "com.eulerity.todo.core.database" }
dependencies {
    implementation(project(":core:model"))
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
}
```

`core/datastore/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.todoapp.android.library)
    alias(libs.plugins.todoapp.android.hilt)
}
android { namespace = "com.eulerity.todo.core.datastore" }
dependencies {
    implementation(project(":core:model"))
    implementation(libs.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
}
```

`core/data/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.todoapp.android.library)
    alias(libs.plugins.todoapp.android.hilt)
}
android { namespace = "com.eulerity.todo.core.data" }
dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.work.runtime.ktx)
    implementation(libs.hilt.work)
    testImplementation(libs.junit)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
}
```

`core/domain/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.todoapp.jvm.library)
    alias(libs.plugins.todoapp.android.hilt)
}
dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:common"))
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
}
```

> Note: `:core:domain` uses `jvm.library` + `hilt`. If KSP on a pure-JVM module
> conflicts with the Hilt plugin during execution, fall back to
> `todoapp.android.library` for this module — it stays effectively pure Kotlin
> either way. This is the one spot to adjust if the build complains; it is the
> Open Question tracked in `00-PRD.md`.

`core/designsystem/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.todoapp.android.library)
    alias(libs.plugins.todoapp.android.library.compose)
}
android { namespace = "com.eulerity.todo.core.designsystem" }
dependencies {
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.core.ktx)
}
```

`core/ui/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.todoapp.android.library)
    alias(libs.plugins.todoapp.android.library.compose)
}
android { namespace = "com.eulerity.todo.core.ui" }
dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:model"))
    implementation(libs.kotlinx.datetime)
}
```

`feature/today/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.todoapp.android.feature)
}
android { namespace = "com.eulerity.todo.feature.today" }
```

`feature/history/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.todoapp.android.feature)
}
android { namespace = "com.eulerity.todo.feature.history" }
```

**Step 3: Add a placeholder source file per module so it compiles**

For each Android library module, create `<module>/src/main/AndroidManifest.xml`
containing only:
```xml
<manifest />
```
For every module, create a placeholder
`<module>/src/main/kotlin/com/eulerity/todo/<pkg>/Placeholder.kt`:
```kotlin
internal val placeholder = Unit
```

**Step 4: Verify the whole project assembles**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`. This proves all eleven modules, the version
catalog, and the convention plugins wire together.

**Step 5: Commit**

```bash
git add .
git commit -m "build: scaffold eleven modules with convention-plugin build files"
```

## Task 0.5: Phase 0 verification gate

**Step 1: Confirm `minSdk` is 28 everywhere**

Run: `grep -rn "minSdk" build-logic/`
Expected: `minSdk = 28` in `KotlinAndroid.kt`, and nowhere is it 24.

**Step 2: Confirm no network permission exists**

Run: `grep -rn "INTERNET" app/ || echo "no internet permission — good"`
Expected: `no internet permission — good`.

**Step 3: Confirm a clean build**

Run: `./gradlew clean assembleDebug`
Expected: `BUILD SUCCESSFUL`.

Commit any fixes, then proceed to `04-phase-1-core.md`.
