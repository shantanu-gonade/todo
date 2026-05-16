package com.eulerity.todo.feature.history.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.eulerity.todo.feature.history.HistoryRoute
import kotlinx.serialization.Serializable

/**
 * Type-safe navigation destination for the History screen.
 *
 * Using a `@Serializable data object` means the route key is automatically
 * serialized/deserialized by Navigation Compose 2.8+ with no string literals.
 */
@Serializable
data object HistoryRouteKey

/**
 * Registers the History screen in the app's [NavGraphBuilder].
 *
 * Call this from the root NavHost alongside [todayScreen] so the app graph
 * remains a single source of truth for all destinations.
 */
fun NavGraphBuilder.historyScreen(onBack: () -> Unit) {
    composable<HistoryRouteKey> {
        HistoryRoute(onBack = onBack)
    }
}
