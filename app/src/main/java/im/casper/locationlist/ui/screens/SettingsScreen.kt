// app/src/main/java/im/casper/locationlist/ui/screens/SettingsScreen.kt
package im.casper.locationlist.ui.screens

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import im.casper.locationlist.data.AppDatabase
import im.casper.locationlist.data.KmlImport
import im.casper.locationlist.data.Location
import im.casper.locationlist.util.ThemeMode
import im.casper.locationlist.util.ThemePreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val DELETE_PHRASE = "this is permanent"

private enum class DeleteKind { LOCATIONS, ALL_DATA }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val dao = remember { AppDatabase.get(context).locationDao() }
    val locations by dao.getAll().collectAsState(initial = emptyList())

    val themeMode by ThemePreference.mode

    var importGroup by remember { mutableStateOf("") }
    var importing by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    var pendingDelete by remember { mutableStateOf<DeleteKind?>(null) }
    var confirmText by remember { mutableStateOf("") }

    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0"
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            importing = true
            status = null
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val parsed = KmlImport.parse(context, uri)
                        val group = importGroup.trim()
                        val withGroup = if (group.isBlank()) {
                            parsed
                        } else {
                            parsed.map { it.copy(groups = (it.groups + group).distinct()) }
                        }
                        val seen = dao.getAllOnce().map { dedupKey(it) }.toHashSet()
                        var imported = 0
                        var skipped = 0
                        for (loc in withGroup) {
                            val key = dedupKey(loc)
                            if (key in seen) {
                                skipped++
                            } else {
                                dao.insert(loc)
                                seen.add(key)
                                imported++
                            }
                        }
                        Triple(withGroup.size, imported, skipped)
                    }
                }
                importing = false
                status = result.fold(
                    onSuccess = { (total, imported, skipped) ->
                        when {
                            total == 0 -> "No placemarks with coordinates were found in that file."
                            imported == 0 -> "All $skipped location(s) were already imported."
                            skipped == 0 -> "Imported $imported location(s)."
                            else -> "Imported $imported, skipped $skipped duplicate(s)."
                        }
                    },
                    onFailure = { "Import failed: ${it.message}" },
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Appearance", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = themeMode == ThemeMode.SYSTEM,
                            onClick = { ThemePreference.set(context, ThemeMode.SYSTEM) },
                            label = { Text("System") },
                        )
                        FilterChip(
                            selected = themeMode == ThemeMode.LIGHT,
                            onClick = { ThemePreference.set(context, ThemeMode.LIGHT) },
                            label = { Text("Light") },
                        )
                        FilterChip(
                            selected = themeMode == ThemeMode.DARK,
                            onClick = { ThemePreference.set(context, ThemeMode.DARK) },
                            label = { Text("Dark") },
                        )
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Import locations", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Import placemarks from a KML or KMZ file. Each file folder becomes a " +
                                "group; you can also tag every imported location with a group below. " +
                                "Duplicates already saved are skipped.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = importGroup,
                        onValueChange = { importGroup = it },
                        label = { Text("Group for imported locations (optional)") },
                        singleLine = true,
                        enabled = !importing,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { picker.launch(arrayOf("*/*")) },
                            enabled = !importing,
                        ) { Text("Choose KML / KMZ file") }
                        if (importing) {
                            Spacer(Modifier.width(16.dp))
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                    status?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("About", style = MaterialTheme.typography.titleMedium)
                    Text("LocationList", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Version $versionName",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "View source on GitHub",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            uriHandler.openUri("https://github.com/chris-casper/LocationList")
                        },
                    )
                    Text(
                        "${locations.size} saved location(s).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Text(
                        "Danger Zone",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Delete every saved location. This cannot be undone. Export your " +
                                    "locations first from the Share / Export screen if you might want " +
                                    "them back.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = { confirmText = ""; pendingDelete = DeleteKind.LOCATIONS },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            ),
                        ) { Text("Delete all locations") }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Delete all app data, including saved locations, photos, and " +
                                    "settings. This resets the app to a fresh state and cannot be " +
                                    "undone.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = { confirmText = ""; pendingDelete = DeleteKind.ALL_DATA },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            ),
                        ) { Text("Delete all app data (Locations, Photos, et al)") }
                    }
                }
            }
        }
    }

    pendingDelete?.let { kind ->
        val confirmed = confirmText.trim().equals(DELETE_PHRASE, ignoreCase = true)
        val title: String
        val body: String
        val confirmLabel: String
        when (kind) {
            DeleteKind.LOCATIONS -> {
                title = "Delete all locations?"
                body = "This permanently deletes all ${locations.size} saved location(s) and " +
                        "cannot be undone. If you might want them later, cancel and export from " +
                        "Share / Export first."
                confirmLabel = "Delete locations"
            }
            DeleteKind.ALL_DATA -> {
                title = "Delete all app data?"
                body = "This permanently deletes all locations, photos, and saved settings, " +
                        "resetting the app to a fresh state. This cannot be undone. If you might " +
                        "want your locations later, cancel and export from Share / Export first."
                confirmLabel = "Delete all data"
            }
        }

        AlertDialog(
            onDismissRequest = { pendingDelete = null; confirmText = "" },
            title = { Text(title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(body)
                    OutlinedTextField(
                        value = confirmText,
                        onValueChange = { confirmText = it },
                        label = { Text("Type \"$DELETE_PHRASE\" to confirm") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = confirmed,
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                dao.deleteAll()
                                if (kind == DeleteKind.ALL_DATA) {
                                    File(context.filesDir, "images").deleteRecursively()
                                    File(context.filesDir, "exports").deleteRecursively()
                                    context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                                        .edit().clear().apply()
                                    context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
                                        .edit().clear().apply()
                                }
                            }
                            if (kind == DeleteKind.ALL_DATA) {
                                ThemePreference.mode.value = ThemeMode.SYSTEM
                            }
                            pendingDelete = null
                            confirmText = ""
                            status = if (kind == DeleteKind.LOCATIONS) {
                                "All locations deleted."
                            } else {
                                "All app data deleted."
                            }
                        }
                    },
                ) {
                    Text(
                        confirmLabel,
                        color = if (confirmed) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        },
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null; confirmText = "" }) {
                    Text("Cancel")
                }
            },
        )
    }
}

// Identity for de-duplication: name + coordinates rounded to ~1 meter.
private fun dedupKey(loc: Location): String {
    val lat = loc.latitude?.let { "%.5f".format(it) } ?: "?"
    val lng = loc.longitude?.let { "%.5f".format(it) } ?: "?"
    return "${loc.name.trim().lowercase()}|$lat|$lng"
}
