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

import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Convention plugin that applies Spotless formatting rules consistently
 * across all modules in the project.
 *
 * Formatting rules:
 *  - Kotlin: ktlint with project .editorconfig
 *  - Gradle KTS: ktlint
 *  - Misc: trailing whitespace + newline enforcement
 *
 * Usage: apply plugin "todoapp.spotless" in any module build.gradle.kts,
 * or let the root build.gradle.kts wire it to all subprojects.
 */
class SpotlessConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.diffplug.spotless")

        extensions.configure<SpotlessExtension> {
            // Kotlin source files
            kotlin {
                target("**/*.kt")
                targetExclude("**/build/**/*.kt")
                // Copyright header — applied to every .kt file.
                // The year pattern matches any 4-digit year so existing headers
                // with a different year are not flagged as violations.
                licenseHeader(
                    """
                    /*
                     * Copyright ${'$'}YEAR Eulerity, Inc.
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
                    """.trimIndent(),
                )
                ktlint()
                    .editorConfigOverride(
                        mapOf(
                            "ij_kotlin_allow_trailing_comma" to "true",
                            "ij_kotlin_allow_trailing_comma_on_call_site" to "true",
                        ),
                    )
                trimTrailingWhitespace()
                endWithNewline()
            }

            // Gradle Kotlin DSL scripts
            kotlinGradle {
                target("**/*.kts")
                targetExclude("**/build/**/*.kts")
                ktlint()
                trimTrailingWhitespace()
                endWithNewline()
            }
        }
    }
}
