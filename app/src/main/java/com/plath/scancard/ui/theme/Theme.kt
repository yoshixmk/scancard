package com.plath.scancard.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * ScanCard app theme.
 *
 * Edge-to-Edge note:
 * ComponentActivity.enableEdgeToEdge() (used in MainActivity) automatically handles
 * isAppearanceLightStatusBars / isAppearanceLightNavigationBars based on theme.
 * The SideEffect below is kept for verification and for cases where WindowCompat.enableEdgeToEdge is used.
 * If ComponentActivity's automatic handling is confirmed, this SideEffect is no-op / redundant.
 */
@Composable
fun ScanCardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            // 自動処理を確認: ComponentActivity.enableEdgeToEdge()が自動でアイコン色を制御するため
            // ここはフォールバック/明示的制御。必要に応じてコメントアウト可。
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }
    MaterialTheme(content = content)
}
