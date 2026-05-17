/*
 * Copyright 2026 Eulerity, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
