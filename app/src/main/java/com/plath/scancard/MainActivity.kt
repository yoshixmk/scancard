package com.plath.scancard

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.plath.scancard.ui.ScanCardNavHost
import com.plath.scancard.ui.theme.ScanCardTheme
import dagger.hilt.android.AndroidEntryPoint

import android.content.Intent
import com.plath.scancard.ui.NavHolder

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
        setContent {
            ScanCardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // TODO(IMP-05): Replace with NavigationSuiteScaffold(calculateFromAdaptiveInfo)
                    // Procedure (Enable after IMP-02 Navigation3 migration):
                    // 1. Uncomment adaptive 3 dependencies in app/build.gradle.kts
                    // 2. import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
                    //    import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
                    //    import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
                    //    import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
                    // 3. Replace Scaffold with NavigationSuiteScaffold using the following template:
                    // ```
                    // var isNavBarVisible by remember { mutableStateOf(true) }
                    // val scaffoldState = rememberNavigationSuiteScaffoldState()
                    // val adaptiveInfo = currentWindowAdaptiveInfo()
                    // NavigationSuiteScaffold(
                    //     navigationSuiteItems = {

                    //         // item(icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") },
                    //         //      selected = currentRoute == Screen.Home.route,
                    //         //      onClick = { navController.navigate(Screen.Home.route) })
                    //     },
                    //     layoutType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(adaptiveInfo),
                    //     state = scaffoldState
                    // ) {
                    //     ScanCardNavHost(navController = navController)
                    // }
                    // LaunchedEffect(isNavBarVisible) {
                    //     if (isNavBarVisible) scaffoldState.show() else scaffoldState.hide()
                    // }
                    // ```
                    // Currently fixed as Scaffold (MainActivity.kt:22-31) — commented out to avoid breaking the build.
                    val navController = rememberNavController()
                    ScanCardNavHost(navController = navController)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Resolve singleTop + scancard://deck deepLink even in warm-start
        // Since ScanCardNavHost's NavController is in Compose, use handleDeepLink via Holder
        NavHolder.navController?.handleDeepLink(intent)
    }
}
