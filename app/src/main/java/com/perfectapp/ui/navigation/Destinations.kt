package com.perfectapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Backup
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Destination(val route: String, val label: String, val icon: ImageVector) {
    data object Home : Destination("home", "Home", Icons.Filled.Home)
    data object Health : Destination("health", "Health", Icons.Filled.Favorite)
    data object Diet : Destination("diet", "Diet", Icons.Filled.Restaurant)
    data object Wealth : Destination("wealth", "Wealth", Icons.Filled.AccountBalance)
    data object Calendar : Destination("calendar", "Calendar", Icons.Filled.CalendarMonth)
    data object More : Destination("more", "More", Icons.Filled.MoreHoriz)
    data object Widgets : Destination("widgets", "Widgets", Icons.Filled.Home)
    data object Car : Destination("car", "Car", Icons.Filled.DirectionsCar)
    data object Renewals : Destination("renewals", "Renewals", Icons.Filled.EventRepeat)
    data object Search : Destination("search", "Search", Icons.Filled.Search)
    data object Notifications : Destination("notifications", "Alerts", Icons.Filled.Notifications)
    data object Backup : Destination("backup", "Backup", Icons.Filled.Backup)
    data object Settings : Destination("settings", "Settings", Icons.Filled.Settings)
}

val bottomNavItems = listOf(
    Destination.Home,
    Destination.Wealth,
    Destination.Calendar,
    Destination.Health,
    Destination.Diet,
    Destination.Car,
    Destination.Renewals,
    Destination.Widgets,
    Destination.Search,
    Destination.Notifications,
    Destination.Backup,
    Destination.Settings
)
