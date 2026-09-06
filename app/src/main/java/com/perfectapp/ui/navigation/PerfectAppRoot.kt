package com.perfectapp.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
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
fun PerfectAppRoot(container: AppContainer, widgetRoute: String? = null) {
    val settings by container.settingsRepository.settings.collectAsState(initial = com.perfectapp.data.repository.AppSettings())
    if (!settings.onboardingComplete) { OnboardingScreen(container); return }
    val navController = rememberNavController()
    androidx.compose.runtime.LaunchedEffect(widgetRoute) {
        if (widgetRoute != null && (widgetRoute in setOf("home", "diet", "wealth", "wealth/add-transaction", "calendar", "calendar/add", "calendar/birthday", "calendar/university", "renewals", "widgets", "diet/add") || widgetRoute.matches(Regex("calendar/edit/[0-9]+")))) {
            navController.navigate(widgetRoute) { launchSingleTop = true }
        }
    }

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            Surface(tonalElevation = 3.dp) {
              Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
                Text("Explore · swipe for more tabs", modifier = Modifier.padding(start = 20.dp, top = 8.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(8.dp)) {
                bottomNavItems.forEach { destination ->
                    val selected = currentRoute?.substringBefore('/') == destination.route
                    val tint by animateColorAsState(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface, label = "Tab selection")
                    Column(
                        modifier = Modifier.width(84.dp).heightIn(min = 64.dp).clip(RoundedCornerShape(20.dp)).background(tint).selectable(
                        selected = selected,
                        role = Role.Tab,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = destination != Destination.Home
                            }
                        }),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(destination.icon, contentDescription = null, tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(destination.label, style = MaterialTheme.typography.labelSmall, color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
              }
              }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(tween(220)) },
            exitTransition = { fadeOut(tween(140)) }
        ) {
            composable(Destination.Home.route) { HomeScreen(container = container) }
            composable("widgets") { com.perfectapp.ui.widgets.WidgetsScreen() }
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
                    onAddTransaction = { navController.navigate("wealth/add-transaction") }
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
                    onAddBirthday = { navController.navigate("calendar/birthday") },
                    onAddUniversity = { navController.navigate("calendar/university") },
                    onEditEvent = { id -> navController.navigate("calendar/edit/$id") }
                )
            }
            composable("calendar/university") {
                AddEventScreen(repository = container.calendarRepository, onSaved = { navController.popBackStack() }, initialType = com.perfectapp.data.entities.CalendarItemType.UNIVERSITY_LESSON)
            }
            composable("calendar/birthday") {
                AddEventScreen(repository = container.calendarRepository,
                    onSaved = { navController.popBackStack() },
                    initialType = com.perfectapp.data.entities.CalendarItemType.BIRTHDAY)
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
                    wealthRepository = container.wealthRepository,
                    onAddAutomatic = { navController.navigate("wealth/add-subscription") },
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
                    onAddOrEditCar = { navController.navigate("car/add") },
                    onEditCar = { id -> navController.navigate("car/edit/$id") },
                    onAddMaintenance = { carId -> navController.navigate("car/add-maintenance/$carId") },
                    onAddCarRenewal = { carId -> navController.navigate("car/renewal/$carId") }
                )
            }
            composable("car/edit/{carId}", arguments = listOf(navArgument("carId") { type = NavType.LongType })) { entry ->
                AddOrEditCarScreen(repository = container.carRepository, carId = entry.arguments?.getLong("carId"), onSaved = { navController.popBackStack() })
            }
            composable("car/add") {
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
