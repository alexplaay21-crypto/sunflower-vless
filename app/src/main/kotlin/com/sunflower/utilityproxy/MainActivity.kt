package com.sunflower.utilityproxy

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sunflower.utilityproxy.data.SettingsRepository
import com.sunflower.utilityproxy.ui.navigation.SunflowerNavHost
import com.sunflower.utilityproxy.ui.theme.SunflowerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * launchMode="singleTask" в манифесте — чтобы deep link (vless://, и т.п.),
 * открытый пока приложение уже на переднем плане, шёл через onNewIntent(),
 * а не создавал новый экземпляр Activity поверх старого.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private var pendingDeepLink by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingDeepLink = intent?.dataString
        setContent {
            val theme by settingsRepository.theme.collectAsState(initial = "system")
            SunflowerTheme(theme = theme) {
                SunflowerNavHost(
                    pendingDeepLink = pendingDeepLink,
                    onDeepLinkHandled = { pendingDeepLink = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDeepLink = intent.dataString
    }
}
