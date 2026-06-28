// app/src/main/java/im/casper/locationlist/ui/screens/SettingsScreen.kt
package im.casper.locationlist.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import im.casper.locationlist.data.AppDatabase
import im.casper.locationlist.data.KmlImport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { AppDatabase.get(context).locationDao() }
    val locations by dao.getAll().collectAsState(initial = emptyList())

    var importGroup by remember { mutableStateOf("") }
    var importing by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

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
                        val toInsert = if (group.isBlank()) {
                            parsed
                        } else {
                            parsed.map { it.copy(groups = (it.groups + group).distinct()) }
                        }
                        toInsert.forEach { dao.insert(it) }
                        toInsert.size
                    }
                }
                importing = false
                status = result.fold(
                    onSuccess = { count ->
                        if (count == 0) "No placemarks with coordinates were found in that file."
                        else "Imported $count location(s)."
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
                    Text("Import locations", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Import placemarks from a KML or KMZ file. Each file folder becomes a " +
                                "group; you can also tag every imported location with a group below.",
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
                        "${locations.size} saved location(s).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
