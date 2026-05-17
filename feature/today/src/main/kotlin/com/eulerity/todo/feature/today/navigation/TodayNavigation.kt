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

@file:Suppress("MatchingDeclarationName") // File intentionally groups route key + nav builder extension

package com.eulerity.todo.feature.today.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.eulerity.todo.feature.today.TodayRoute
import kotlinx.serialization.Serializable

/**
 * Type-safe navigation destination for the Today screen.
 *
 * Using a `@Serializable data object` means the route key is automatically
 * serialized/deserialized by Navigation Compose 2.8+ with no string literals.
 */
@Serializable
data object TodayRouteKey

/**
 * Registers the Today screen in the app's [NavGraphBuilder].
 *
 * Call this from the root NavHost so the app graph remains a single
 * source of truth for all destinations.
 */
fun NavGraphBuilder.todayScreen(
    onNavigateToHistory: () -> Unit,
) {
    composable<TodayRouteKey> {
        TodayRoute(onNavigateToHistory = onNavigateToHistory)
    }
}
