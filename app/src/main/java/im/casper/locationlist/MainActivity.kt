// app/src/main/java/im/casper/locationlist/MainActivity.kt
package im.casper.locationlist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import im.casper.locationlist.navigation.AppNavHost
import im.casper.locationlist.ui.theme.LocationListTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LocationListTheme {
                AppNavHost()
            }
        }
    }
}