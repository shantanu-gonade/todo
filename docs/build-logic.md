# Build Logic

The `build-logic/` directory is a Gradle [included build](https://docs.gradle.org/current/userguide/composite_builds.html)
that provides **convention plugins** — reusable Gradle plugins that apply a consistent,
pre-configured set of build settings to every module. This eliminates the boilerplate
of copying `compileSdk`, `minSdk`, JVM target, Compose options, and KSP config into
every `build.gradle.kts`.

---

## Directory layout

```
build-logic/
└── convention/
    ├── build.gradle.kts          ← builds the plugins themselves
    └── src/main/kotlin/
        ├── AndroidApplicationConventionPlugin.kt
        ├── AndroidLibraryConventionPlugin.kt
        ├── AndroidLibraryComposeConventionPlugin.kt
        ├── AndroidFeatureConventionPlugin.kt
        ├── AndroidHiltConventionPlugin.kt
        ├── AndroidRoomConventionPlugin.kt
        └── KotlinLibraryConventionPlugin.kt
```

`settings.gradle.kts` at the root includes the build:

```kotlin
includeBuild("build-logic")
```

---

## Available convention plugins

### `todoapp.android.application`

Applied only to the `:app` module. Sets:

- `compileSdk = 36`
- `minSdk = 28`
- `targetSdk = 35`
- `applicationId = "com.eulerity.todo"`
- `versionCode`, `versionName`
- Java source/target compatibility = 17
- Kotlin JVM target = 17
- Applies `com.android.application` and Kotlin Android plugin

### `todoapp.android.library`

Applied to every `core/*` and `feature/*` Android module. Sets the same SDK and JVM
targets as the application plugin but uses `com.android.library`. Also disables
`buildConfig` generation (not needed in library modules).

### `todoapp.android.library.compose`

Extends `todoapp.android.library` with Compose:

- Enables `buildFeatures.compose = true`
- Sets `composeOptions.kotlinCompilerExtensionVersion` from the version catalog
- Applies the Compose Compiler Gradle plugin (AGP 9 — no separate plugin needed
  since the Kotlin Compose compiler plugin is bundled)
- Adds the Compose BOM and `ui-tooling-preview` to `debugImplementation`

### `todoapp.android.feature`

The one-stop plugin for any `feature/*` module. Applies:

1. `todoapp.android.library`
2. `todoapp.android.library.compose`
3. `todoapp.android.hilt`
4. `org.jetbrains.kotlin.plugin.serialization` (for type-safe Nav routes)

And adds these dependencies automatically:

```
implementation(":core:ui")
implementation(":core:designsystem")
implementation(":core:domain")
implementation(":core:model")
implementation(":core:common")
implementation(libs.androidx.lifecycle.viewModelCompose)
implementation(libs.androidx.hilt.navigation.compose)
implementation(libs.androidx.navigation.compose)
implementation(libs.kotlinx.serialization.json)
```

A new feature module only needs:

```kotlin
// feature/myfeature/build.gradle.kts
plugins {
    alias(libs.plugins.todoapp.android.feature)
    alias(libs.plugins.todoapp.android.library.compose)
}
```

### `todoapp.android.hilt`

Applies `com.google.dagger.hilt.android` and `com.google.devtools.ksp`. Adds:

```
implementation(libs.hilt.android)
ksp(libs.hilt.compiler)
```

Can be added to any module that needs DI without pulling in Compose or feature
dependencies.

### `todoapp.android.room`

Applies KSP and the Room compiler. Configures the schema export directory:

```kotlin
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}
```

Adds:

```
implementation(libs.androidx.room.runtime)
implementation(libs.androidx.room.ktx)
ksp(libs.androidx.room.compiler)
```

### `todoapp.kotlin.library`

For pure Kotlin modules (`:core:model`, `:core:domain`) with no Android dependency.
Applies `org.jetbrains.kotlin.jvm` and sets the JVM target to 17.

---

## Version catalog (`gradle/libs.versions.toml`)

All library versions and plugin IDs are declared in the version catalog. Key entries:

```toml
[versions]
agp                   = "9.0.1"
kotlin                = "2.1.0"
compose-bom           = "2026.05.00"
navigation-compose    = "2.8.5"
hilt                  = "2.59.2"
room                  = "2.8.4"
datastore             = "1.1.1"
kotlinx-datetime      = "0.6.1"
coroutines            = "1.9.0"
workmanager           = "2.10.0"
turbine               = "1.2.0"
robolectric           = "4.14"

[plugins]
todoapp-android-application         = { id = "todoapp.android.application",         version = "unspecified" }
todoapp-android-library             = { id = "todoapp.android.library",             version = "unspecified" }
todoapp-android-library-compose     = { id = "todoapp.android.library.compose",     version = "unspecified" }
todoapp-android-feature             = { id = "todoapp.android.feature",             version = "unspecified" }
todoapp-android-hilt                = { id = "todoapp.android.hilt",               version = "unspecified" }
todoapp-android-room                = { id = "todoapp.android.room",               version = "unspecified" }
todoapp-kotlin-library              = { id = "todoapp.kotlin.library",             version = "unspecified" }
```

---

## Adding a new library module

1. Create the directory and `build.gradle.kts`:

   ```kotlin
   // core/analytics/build.gradle.kts
   plugins {
       alias(libs.plugins.todoapp.android.library)
       alias(libs.plugins.todoapp.android.hilt)   // only if DI needed
   }

   android {
       namespace = "com.eulerity.todo.core.analytics"
   }
   ```

2. Add the module to `settings.gradle.kts`:

   ```kotlin
   include(":core:analytics")
   ```

3. Depend on it from other modules:

   ```kotlin
   implementation(projects.core.analytics)
   ```

No SDK versions, JVM targets, or compiler flags need to be repeated — the convention
plugin handles everything.

## Adding a new feature module

1. Create `feature/myfeature/build.gradle.kts`:

   ```kotlin
   plugins {
       alias(libs.plugins.todoapp.android.feature)
   }

   android {
       namespace = "com.eulerity.todo.feature.myfeature"
   }
   ```

2. Add to `settings.gradle.kts`:

   ```kotlin
   include(":feature:myfeature")
   ```

3. Register the screen in `:app`'s `TodoNavHost`.

The `todoapp.android.feature` plugin pulls in all core modules, Hilt Navigation
Compose, Navigation Compose, and serialization automatically.

---

## CI integration

Spotless and Detekt run on every PR:

```yaml
# .github/workflows/ci.yml
- run: ./gradlew spotlessCheck
- run: ./gradlew detekt
- run: ./gradlew test
```

Run locally before committing:

```bash
./gradlew spotlessApply   # auto-fix formatting
./gradlew detekt          # static analysis
./gradlew test            # unit tests
```
