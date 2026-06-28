// app/src/main/java/im/casper/locationlist/navigation/AppNavHost.kt
package im.casper.locationlist.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import im.casper.locationlist.ui.home.HomeScreen
import im.casper.locationlist.ui.screens.CreateLocationScreen
import im.casper.locationlist.ui.screens.LocationDetailScreen
import im.casper.locationlist.ui.screens.LocationListScreen
import im.casper.locationlist.ui.screens.MapScreen
import im.casper.locationlist.ui.screens.SettingsScreen
import im.casper.locationlist.ui.screens.ShareScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(onNavigate = { route -> navController.navigate(route) })
        }
        composable(Routes.LOCATION_LIST) {
            LocationListScreen(
                onBack = { navController.popBackStack() },
                onLocationClick = { id -> navController.navigate(Routes.locationDetail(id)) },
            )
        }
        composable(Routes.CREATE_LOCATION) {
            CreateLocationScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = "${Routes.LOCATION_DETAIL}/{locationId}",
            arguments = listOf(navArgument("locationId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("locationId") ?: 0L
            LocationDetailScreen(locationId = id, onBack = { navController.popBackStack() })
        }
        composable(Routes.MAP) {
            MapScreen(
                onBack = { navController.popBackStack() },
                onLocationClick = { id -> navController.navigate(Routes.locationDetail(id)) },
            )
        }
        composable(Routes.SHARE) {
            ShareScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}