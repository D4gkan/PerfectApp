package com.perfectapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Destination(val route: String, val label: String, val icon: ImageVector) {
    data object Home : Destination("home", "Home", Icons.Filled.Home)
    data object Health : Destination("health", "Health", Icons.Filled.Favorite)
    data object Diet : Destination("diet", "Diet", Icons.Filled.Restaurant)
    data object Wealth : Destination("wealth", "Wealth", Icons.Filled.AccountBalance)
    data object Calendar : Destination("calendar", "Calendar", Icons.Filled.CalendarMonth)
    data object More : Destination("more", "More", Icons.Filled.MoreHoriz)
}

val bottomNavItems = listOf(
    Destination.Home,
    Destination.Health,
    Destination.Diet,
    Destination.Wealth,
    Destination.Calendar,
    Destination.More
)
