// app/src/main/java/im/casper/locationlist/MainActivity.kt
package im.casper.locationlist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import im.casper.locationlist.navigation.AppNavHost
import im.casper.locationlist.ui.theme.LocationListTheme
import im.casper.locationlist.util.ThemeMode
import im.casper.locationlist.util.ThemePreference

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemePreference.init(this)
        enableEdgeToEdge()
        setContent {
            val mode by ThemePreference.mode
            val dark = when (mode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            LocationListTheme(darkTheme = dark) {
                AppNavHost()
            }
        }
    }
}
