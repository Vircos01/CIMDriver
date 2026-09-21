package com.cimdriver.app.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.cimdriver.app.ui.viewmodel.TripsViewModel
import com.cimdriver.app.ui.viewmodel.VehiclesViewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import kotlinx.coroutines.launch
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.cimdriver.app.ui.screens.DashboardScreen
import com.cimdriver.app.ui.screens.SettingsScreen
import com.cimdriver.app.ui.screens.TripDetailScreen
import com.cimdriver.app.ui.screens.TripsScreen
import com.cimdriver.app.ui.screens.VehiclesScreen
import com.cimdriver.app.ui.screens.AddTripScreen
import androidx.compose.ui.res.stringResource
import androidx.navigation.toRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.annotation.StringRes
import com.cimdriver.app.R

sealed class Screen(val route: Any, @StringRes val titleRes: Int, val icon: ImageVector) {
    object Dashboard : Screen(DashboardRoute, R.string.dashboard, Icons.Filled.Home)
    object Trips : Screen(TripsRoute, R.string.trips, Icons.AutoMirrored.Filled.List)
    object Vehicles : Screen(VehiclesRoute, R.string.vehicles, Icons.Filled.DirectionsCar)
    object WorkHours : Screen(WorkHoursRoute, R.string.work_hours, Icons.Filled.Schedule)
    object Settings : Screen(SettingsRoute, R.string.settings, Icons.Filled.Settings)
    object AddressBook : Screen(AddressBookRoute, R.string.address_book, Icons.Filled.Contacts)
    object About : Screen(AboutRoute, R.string.about, Icons.Filled.Info)
    object Help : Screen(HelpRoute, R.string.help, Icons.AutoMirrored.Filled.Help)
}

val bottomNavigationItems = listOf(
    Screen.Dashboard,
    Screen.Trips,
    Screen.AddressBook,
    Screen.WorkHours
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(tripsViewModel: TripsViewModel = hiltViewModel(), vehiclesViewModel: VehiclesViewModel = hiltViewModel()) {
    val trips by tripsViewModel.trips.collectAsState(initial = emptyList())
    val toReviewCount = trips.count { it.status == "TO_REVIEW" }
    
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                // Banner spanning full width at the very top
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.cimdriver.app.R.drawable.menu_banner),
                    contentDescription = "CIMDriver Banner",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.dashboard)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(DashboardRoute)
                    },
                    icon = { Icon(Screen.Dashboard.icon, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Auto's") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(VehiclesRoute)
                    },
                    icon = { Icon(Screen.Vehicles.icon, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.settings)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(SettingsRoute)
                    },
                    icon = { Icon(Screen.Settings.icon, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.help)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(HelpRoute)
                    },
                    icon = { Icon(Screen.Help.icon, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.about)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(AboutRoute)
                    },
                    icon = { Icon(Screen.About.icon, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            
            // Only show bottom navigation on main tabs
            val showBottomNav = bottomNavigationItems.any { screen ->
                currentDestination?.hierarchy?.any { it.hasRoute(screen.route::class) } == true
            }
            
            if (showBottomNav) {
                NavigationBar {
                    bottomNavigationItems.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                if (screen == Screen.Dashboard && toReviewCount > 0) {
                                    BadgedBox(
                                        badge = {
                                            Badge {
                                                Text(toReviewCount.toString())
                                            }
                                        }
                                    ) {
                                        Icon(screen.icon, contentDescription = stringResource(screen.titleRes))
                                    }
                                } else {
                                    Icon(screen.icon, contentDescription = stringResource(screen.titleRes))
                                }
                            },
                            label = { Text(stringResource(screen.titleRes)) },
                            selected = currentDestination?.hierarchy?.any { it.hasRoute(screen.route::class) } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = false
                                    }
                                    launchSingleTop = true
                                    restoreState = false
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        // Monthly odometer verification dialog
        val odometerCheckRequest by com.cimdriver.app.service.OdometerCheckStore.request.collectAsState()
        val vehicles by vehiclesViewModel.vehicles.collectAsState(initial = emptyList())

        if (odometerCheckRequest.showDialog && odometerCheckRequest.vehicleId != null) {
            val checkVehicle = vehicles.find { it.id == odometerCheckRequest.vehicleId }
            if (checkVehicle != null) {
                var odometerInput by remember(checkVehicle.id) {
                    mutableStateOf(checkVehicle.odometerCurrent.toString())
                }
                AlertDialog(
                    onDismissRequest = {
                        com.cimdriver.app.service.OdometerCheckStore.dismiss()
                    },
                    title = { Text(stringResource(R.string.verify_odometer_title)) },
                    text = {
                        Column {
                            Text(
                                text = stringResource(R.string.verify_odometer_desc, checkVehicle.make, checkVehicle.model, checkVehicle.licensePlate),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Huidige geregistreerde stand: ${String.format(java.util.Locale.US, "%,d", checkVehicle.odometerCurrent).replace(',', '.')} km",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = odometerInput,
                                onValueChange = { odometerInput = it },
                                label = { Text(stringResource(R.string.new_odometer)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                ),
                                suffix = { Text("km") }
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val corrected = odometerInput.toIntOrNull() ?: checkVehicle.odometerCurrent
                            vehiclesViewModel.confirmOdometerCheck(checkVehicle, corrected)
                            com.cimdriver.app.service.OdometerCheckStore.dismiss()
                        }) {
                            Text("Bevestigen")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            // Confirm with current value (no change, but stamp the check)
                            vehiclesViewModel.confirmOdometerCheck(checkVehicle, checkVehicle.odometerCurrent)
                            com.cimdriver.app.service.OdometerCheckStore.dismiss()
                        }) {
                            Text("Klopt al")
                        }
                    }
                )
            } else if (vehicles.isNotEmpty()) {
                // Vehicle not found (deleted?), dismiss only if we actually loaded the list
                com.cimdriver.app.service.OdometerCheckStore.dismiss()
            }
        }

        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            val onOpenDrawer: () -> Unit = { scope.launch { drawerState.open() } }
            
            composable<DashboardRoute> { 
                DashboardScreen(
                    onOpenDrawer = onOpenDrawer,
                    onNavigateToTrips = {
                        navController.navigate(TripsRoute) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = false
                        }
                    },
                    onNavigateToWorkHours = {
                        navController.navigate(WorkHoursRoute) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = false
                        }
                    },
                    onNavigateToDiagnostics = {
                        navController.navigate(DiagnosticsRoute)
                    }
                ) 
            }
            composable<TripsRoute> { 
                TripsScreen(
                    onOpenDrawer = onOpenDrawer,
                    onTripClick = { trip ->
                        navController.navigate(TripDetailRoute(trip.id))
                    },
                    onAddTrip = {
                        navController.navigate(AddTripRoute())
                    }
                ) 
            }
            composable<VehiclesRoute> { 
                VehiclesScreen(
                    onOpenDrawer = onOpenDrawer,
                    onAddVehicle = { navController.navigate(AddVehicleRoute()) },
                    onEditVehicle = { vehicleId -> navController.navigate(AddVehicleRoute(vehicleId = vehicleId)) }
                ) 
            }
            composable<SettingsRoute> { 
                SettingsScreen(
                    onOpenDrawer = onOpenDrawer,
                    navController = navController
                ) 
            }
            composable<TripClassificationSettingsRoute> {
                com.cimdriver.app.ui.screens.settings.TripClassificationSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable<TrackingPrivacySettingsRoute> {
                com.cimdriver.app.ui.screens.settings.TrackingPrivacySettingsScreen(onBack = { navController.popBackStack() })
            }
            composable<NotificationSettingsRoute> {
                com.cimdriver.app.ui.screens.settings.NotificationSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable<DataManagementRoute> {
                com.cimdriver.app.ui.screens.settings.DataManagementScreen(onBack = { navController.popBackStack() })
            }
            composable<WorkDaysEditorRoute> {
                com.cimdriver.app.ui.screens.settings.WorkDaysEditorScreen(onBack = { navController.popBackStack() })
            }
            composable<WorkHoursRoute> { 
                com.cimdriver.app.ui.screens.WorkHoursScreen(
                    onOpenDrawer = onOpenDrawer,
                    onAddWorkDay = { navController.navigate(AddWorkDayRoute()) },
                    onEditWorkDay = { id -> navController.navigate(AddWorkDayRoute(workDayId = id)) }
                ) 
            }
            composable<AddressBookRoute> { 
                com.cimdriver.app.ui.screens.AddressBookScreen(
                    onOpenDrawer = onOpenDrawer,
                    onAddAddress = { navController.navigate(AddAddressRoute()) },
                    onEditAddress = { addressId -> navController.navigate(AddAddressRoute(addressId = addressId)) }
                ) 
            }
            
            composable<AddWorkDayRoute> { backStackEntry ->
                val args = backStackEntry.toRoute<AddWorkDayRoute>()
                com.cimdriver.app.ui.screens.AddWorkDayScreen(
                    workDayId = args.workDayId,
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable<TripDetailRoute> { backStackEntry ->
                val args = backStackEntry.toRoute<TripDetailRoute>()
                TripDetailScreen(
                    tripId = args.tripId,
                    onBack = { navController.popBackStack() },
                    onEditTrip = { navController.navigate(AddTripRoute(tripId = it)) },
                    onDuplicateTrip = { id, isReturn ->
                        navController.navigate(AddTripRoute(copyFrom = id, isReturn = isReturn))
                    }
                )
            }

            composable<AddTripRoute> { backStackEntry ->
                val args = backStackEntry.toRoute<AddTripRoute>()
                AddTripScreen(
                    tripId = args.tripId,
                    copyFromId = args.copyFrom,
                    isReturn = args.isReturn,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<AddVehicleRoute> { backStackEntry ->
                val args = backStackEntry.toRoute<AddVehicleRoute>()
                com.cimdriver.app.ui.screens.AddVehicleScreen(
                    vehicleId = args.vehicleId,
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable<AddAddressRoute> { backStackEntry ->
                val args = backStackEntry.toRoute<AddAddressRoute>()
                com.cimdriver.app.ui.screens.AddAddressScreen(
                    addressId = args.addressId,
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable<AboutRoute> {
                com.cimdriver.app.ui.screens.AboutScreen(onOpenDrawer = onOpenDrawer)
            }
            
            composable<HelpRoute> {
                com.cimdriver.app.ui.screens.HelpScreen(onOpenDrawer = onOpenDrawer)
            }
            composable<DiagnosticsRoute> {
                com.cimdriver.app.ui.screens.DiagnosticsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
        }
    }
}
