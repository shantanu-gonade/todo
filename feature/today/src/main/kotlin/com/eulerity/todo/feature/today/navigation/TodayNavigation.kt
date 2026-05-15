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
