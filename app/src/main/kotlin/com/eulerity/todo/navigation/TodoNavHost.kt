package com.eulerity.todo.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.eulerity.todo.feature.history.navigation.HistoryRouteKey
import com.eulerity.todo.feature.history.navigation.historyScreen
import com.eulerity.todo.feature.today.navigation.TodayRouteKey
import com.eulerity.todo.feature.today.navigation.todayScreen

/**
 * Root navigation host for the Todo app.
 *
 * Compose rule: NavController is created here and passed down only through
 * lambda callbacks — screens never hold a NavController reference directly.
 * This keeps screens stateless and testable in isolation.
 */
@Composable
fun TodoNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = TodayRouteKey,
    ) {
        todayScreen(
            onNavigateToHistory = { navController.navigate(HistoryRouteKey) },
        )
        historyScreen(
            onBack = { navController.popBackStack() },
        )
    }
}
