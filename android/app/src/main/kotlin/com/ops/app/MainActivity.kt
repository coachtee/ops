package com.ops.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ops.app.ui.navigation.OpsDestinations
import com.ops.app.ui.navigation.OpsNavGraph
import com.ops.app.ui.navigation.rememberOpsNavController
import com.ops.app.ui.theme.OpsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OpsTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    OpsApp()
                }
            }
        }
    }
}

private data class BottomNavItem(val route: String, val label: String, val icon: ImageVector)

private val BOTTOM_NAV_ITEMS = listOf(
    BottomNavItem(OpsDestinations.HOME, "Home", Icons.Filled.Home),
    BottomNavItem(OpsDestinations.LEADS, "Leads", Icons.Filled.Handshake),
    BottomNavItem(OpsDestinations.CUSTOMERS_PICKABLE, "Customers", Icons.Filled.Group),
    BottomNavItem(OpsDestinations.MONEY, "Money", Icons.Filled.Payments),
)

/**
 * Bottom navigation, four destinations, per DISCOVERY.md section 5's IA.
 * Shown only on those four top-level screens — every deeper screen (lead
 * detail, quote edit, sync status, ...) is reached by pushing onto the back
 * stack and has its own back arrow, keeping the "nothing nested more than
 * two levels deep" rule visible in the navigation itself.
 */
@Composable
fun OpsApp() {
    val navController = rememberOpsNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = BOTTOM_NAV_ITEMS.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    BOTTOM_NAV_ITEMS.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                val target = if (item.route == OpsDestinations.CUSTOMERS_PICKABLE) {
                                    OpsDestinations.customers("none")
                                } else {
                                    item.route
                                }
                                navController.navigate(target) {
                                    popUpTo(OpsDestinations.HOME) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        // This outer Scaffold only ever has a bottomBar (no topBar of its
        // own), so `padding` here is just the bottom nav bar's height. Each
        // screen's own inner Scaffold (see ui/home, ui/leads, ...) separately
        // reserves space for its own TopAppBar — the two compose without
        // double-padding since they're insetting opposite edges.
        OpsNavGraph(navController = navController, modifier = Modifier.fillMaxSize().padding(padding))
    }
}
