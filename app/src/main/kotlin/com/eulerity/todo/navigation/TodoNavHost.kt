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
