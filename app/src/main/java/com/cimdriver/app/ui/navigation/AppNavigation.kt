package com.cimdriver.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cimdriver.app.ui.screens.VehiclesScreen

object NavRoutes {
    const val VEHICLE_LIST = "vehicle_list"
}

@Composable
fun AppNavigation(
    startDestination: String = NavRoutes.VEHICLE_LIST,
    onNavigateToVehicleList: () -> Unit = {}
) {
    NavHost(
        navController = rememberNavController(),
        startDestination = startDestination
    ) {
        composable(route = NavRoutes.VEHICLE_LIST) {
            VehiclesScreen(onAddVehicle = onNavigateToVehicleList)
        }
    }
}
