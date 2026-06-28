// app/src/main/java/im/casper/locationlist/util/ThemePreference.kt
package im.casper.locationlist.util

import android.content.Context
import androidx.compose.runtime.mutableStateOf

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * App-wide theme choice. The [mode] is Compose snapshot state, so reading it in a composable
 * (e.g. MainActivity) recomposes when it changes. The value is persisted to SharedPreferences.
 */
object ThemePreference {
    private const val PREFS = "settings"
    private const val KEY = "theme_mode"

    val mode = mutableStateOf(ThemeMode.SYSTEM)

    fun init(context: Context) {
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null)
        mode.value = saved?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM
    }

    fun set(context: Context, newMode: ThemeMode) {
        mode.value = newMode
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, newMode.name).apply()
    }
}
