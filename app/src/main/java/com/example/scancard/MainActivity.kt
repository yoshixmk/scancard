package com.example.scancard

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
import com.example.scancard.ui.ScanCardNavHost
import dagger.hilt.android.AndroidEntryPoint

import android.content.Intent

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // TODO(IMP-05): NavigationSuiteScaffold(calculateFromAdaptiveInfo) に置換
                    // 手順 (IMP-02 Navigation3移行後に有効化):
                    // 1. app/build.gradle.kts の adaptive 3依存のコメントを外す
                    // 2. import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
                    //    import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
                    //    import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
                    //    import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
                    // 3. 下記雛形で Scaffold を NavigationSuiteScaffold に置換:
                    // ```
                    // var isNavBarVisible by remember { mutableStateOf(true) }
                    // val scaffoldState = rememberNavigationSuiteScaffoldState()
                    // val adaptiveInfo = currentWindowAdaptiveInfo()
                    // NavigationSuiteScaffold(
                    //     navigationSuiteItems = {
                    //         // TODO: 各 Screen の NavigationSuiteItem をここへ移設
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
                    // 現状は Scaffold固定のまま (MainActivity.kt:22-31) — ビルドを壊さないためコメント留め。
                    val navController = rememberNavController()
                    ScanCardNavHost(navController = navController)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
