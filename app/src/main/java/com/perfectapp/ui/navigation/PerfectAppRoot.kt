package com.perfectapp.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.perfectapp.AppContainer
import com.perfectapp.ui.screens.calendar.AddEventScreen
import com.perfectapp.ui.screens.calendar.CalendarScreen
import com.perfectapp.ui.screens.calendar.EditEventScreen
import com.perfectapp.ui.screens.car.AddMaintenanceScreen
import com.perfectapp.ui.screens.car.AddOrEditCarScreen
import com.perfectapp.ui.screens.car.CarScreen
import com.perfectapp.ui.screens.diet.AddMealScreen
import com.perfectapp.ui.screens.diet.DietScreen
import com.perfectapp.ui.screens.health.AddMeasurementScreen
import com.perfectapp.ui.screens.health.HealthScreen
import com.perfectapp.ui.screens.health.MeasurementDetailScreen
import com.perfectapp.ui.screens.health.ActivitiesScreen
import com.perfectapp.ui.screens.home.HomeScreen
import com.perfectapp.ui.screens.more.MoreScreen
import com.perfectapp.ui.screens.notifications.NotificationsScreen
import com.perfectapp.ui.screens.renewals.AddRenewalScreen
import com.perfectapp.ui.screens.renewals.RenewalsScreen
import com.perfectapp.ui.screens.settings.SettingsScreen
import com.perfectapp.ui.screens.settings.BackupScreen
import com.perfectapp.ui.screens.wealth.AddAssetScreen
import com.perfectapp.ui.screens.wealth.AddSubscriptionScreen
import com.perfectapp.ui.screens.wealth.AddTransactionScreen
import com.perfectapp.ui.screens.wealth.WealthScreen
import com.perfectapp.ui.screens.search.SearchScreen
import com.perfectapp.ui.screens.onboarding.OnboardingScreen

@Composable
fun PerfectAppRoot(container: AppContainer) {
    val settings by container.settingsRepository.settings.collectAsState(initial = com.perfectapp.data.repository.AppSettings())
    if (!settings.onboardingComplete) { OnboardingScreen(container); return }
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            NavigationBar {
                bottomNavItems.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Destination.Home.route) { HomeScreen(container = container) }
            composable(Destination.Health.route) {
                HealthScreen(
                    repository = container.healthRepository,
                    onAddMeasurement = { navController.navigate("health/add") },
                    onOpenMeasurement = { id -> navController.navigate("health/detail/$id") },
                    onOpenActivities = { navController.navigate("health/activities") }
                )
            }
            composable("health/add") {
                AddMeasurementScreen(
                    repository = container.healthRepository,
                    onSaved = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable("health/detail/{measurementId}", arguments = listOf(navArgument("measurementId") { type = NavType.LongType })) { entry ->
                MeasurementDetailScreen(entry.arguments?.getLong("measurementId") ?: 0L, container.healthRepository)
            }
            composable("health/activities") { ActivitiesScreen(container.healthRepository) }
            composable(Destination.Diet.route) {
                DietScreen(
                    dietRepository = container.dietRepository,
                    waterRepository = container.waterRepository,
                    settingsRepository = container.settingsRepository,
                    onAddMeal = { navController.navigate("diet/add") }
                )
            }
            composable("diet/add") {
                AddMealScreen(
                    dietRepository = container.dietRepository,
                    onSaved = { navController.popBackStack() }
                )
            }
            composable(Destination.Wealth.route) {
                WealthScreen(
                    repository = container.wealthRepository,
                    settingsRepository = container.settingsRepository,
                    onAddAsset = { navController.navigate("wealth/add-asset") },
                    onAddTransaction = { navController.navigate("wealth/add-transaction") },
                    onAddSubscription = { navController.navigate("wealth/add-subscription") }
                )
            }
            composable("wealth/add-subscription") {
                AddSubscriptionScreen(
                    repository = container.wealthRepository,
                    onSaved = { navController.popBackStack() }
                )
            }
            composable("wealth/add-asset") {
                AddAssetScreen(
                    repository = container.wealthRepository,
                    onSaved = { navController.popBackStack() }
                )
            }
            composable("wealth/add-transaction") {
                AddTransactionScreen(
                    repository = container.wealthRepository,
                    onSaved = { navController.popBackStack() }
                )
            }
            composable(Destination.Calendar.route) {
                CalendarScreen(
                    repository = container.calendarRepository,
                    onAddEvent = { navController.navigate("calendar/add") },
                    onEditEvent = { id -> navController.navigate("calendar/edit/$id") }
                )
            }
            composable("calendar/add") {
                AddEventScreen(
                    repository = container.calendarRepository,
                    onSaved = { navController.popBackStack() }
                )
            }
            composable("calendar/edit/{eventId}", arguments = listOf(navArgument("eventId") { type = NavType.LongType })) { entry ->
                EditEventScreen(entry.arguments?.getLong("eventId") ?: 0L, container.calendarRepository) { navController.popBackStack() }
            }
            composable(Destination.More.route) {
                MoreScreen(
                    onCarClick = { navController.navigate("car") },
                    onRenewalsClick = { navController.navigate("renewals") },
                    onNotificationsClick = { navController.navigate("notifications") },
                    onSearchClick = { navController.navigate("search") },
                    onBackupClick = { navController.navigate("backup") },
                    onSettingsClick = { navController.navigate("settings") }
                )
            }
            composable("backup") { BackupScreen(container.backupRepository) { navController.popBackStack() } }
            composable("settings") {
                SettingsScreen(repository = container.settingsRepository, onSaved = { navController.popBackStack() })
            }
            composable("notifications") {
                NotificationsScreen(container.settingsRepository)
            }
            composable("search") { SearchScreen(container) }
            composable("renewals") {
                RenewalsScreen(
                    repository = container.reminderRepository,
                    onAddRenewal = { navController.navigate("renewals/add") }
                )
            }
            composable("renewals/add") {
                AddRenewalScreen(
                    repository = container.reminderRepository,
                    onSaved = { navController.popBackStack() }
                )
            }
            composable("car") {
                CarScreen(
                    repository = container.carRepository,
                    wealthRepository = container.wealthRepository,
                    reminderRepository = container.reminderRepository,
                    onAddOrEditCar = { navController.navigate("car/edit") },
                    onAddMaintenance = { carId -> navController.navigate("car/add-maintenance/$carId") },
                    onAddCarRenewal = { carId -> navController.navigate("car/renewal/$carId") }
                )
            }
            composable("car/edit") {
                AddOrEditCarScreen(
                    repository = container.carRepository,
                    onSaved = { navController.popBackStack() }
                )
            }
            composable(
                "car/add-maintenance/{carId}",
                arguments = listOf(navArgument("carId") { type = NavType.LongType })
            ) { backStackEntry ->
                val carId = backStackEntry.arguments?.getLong("carId") ?: 0L
                AddMaintenanceScreen(
                    repository = container.carRepository,
                    carId = carId,
                    onSaved = { navController.popBackStack() }
                )
            }
            composable("car/renewal/{carId}", arguments = listOf(navArgument("carId") { type = NavType.LongType })) { entry ->
                AddRenewalScreen(repository = container.reminderRepository, linkedCarId = entry.arguments?.getLong("carId"), onSaved = { navController.popBackStack() })
            }
        }
    }
}
