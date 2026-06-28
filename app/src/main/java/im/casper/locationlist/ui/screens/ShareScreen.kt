// app/src/main/java/im/casper/locationlist/ui/screens/ShareScreen.kt
package im.casper.locationlist.ui.screens

import android.content.Intent
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
import androidx.compose.material3.OutlinedButton
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
import androidx.core.content.FileProvider
import im.casper.locationlist.data.AppDatabase
import im.casper.locationlist.data.KmlExport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val EXPORT_MIME = "application/vnd.google-earth.kml+xml"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { AppDatabase.get(context).locationDao() }
    val locations by dao.getAll().collectAsState(initial = emptyList())

    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    val exportableCount = locations.count { it.latitude != null && it.longitude != null }

    // Save-to-file via the system document picker.
    val saver = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(EXPORT_MIME)
    ) { uri ->
        if (uri != null) {
            busy = true
            status = null
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val kml = KmlExport.build(locations)
                        context.contentResolver.openOutputStream(uri)?.use {
                            it.write(kml.toByteArray())
                        } ?: error("Could not open the file for writing")
                    }
                }
                busy = false
                status = result.fold(
                    onSuccess = { "Saved." },
                    onFailure = { "Save failed: ${it.message}" },
                )
            }
        }
    }

    fun shareKml() {
        busy = true
        status = null
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val kml = KmlExport.build(locations)
                    val dir = File(context.filesDir, "exports").apply { mkdirs() }
                    val file = File(dir, "locations.kml")
                    file.writeText(kml)
                    FileProvider.getUriForFile(
                        context, "${context.packageName}.fileprovider", file
                    )
                }
            }
            busy = false
            result.onSuccess { uri ->
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = EXPORT_MIME
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share locations"))
            }.onFailure { status = "Share failed: ${it.message}" }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Share / Export") },
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
                    Text("Export locations", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Export your saved locations as a KML file. Groups become folders, and " +
                                "notes and tags are preserved. Only locations with coordinates are " +
                                "included.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "$exportableCount location(s) ready to export.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(
                            onClick = { shareKml() },
                            enabled = !busy && exportableCount > 0,
                        ) { Text("Share…") }
                        OutlinedButton(
                            onClick = { saver.launch("locations.kml") },
                            enabled = !busy && exportableCount > 0,
                        ) { Text("Save to file…") }
                        if (busy) {
                            Spacer(Modifier.width(8.dp))
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }

                    if (exportableCount == 0) {
                        Text(
                            "Add a location with coordinates first.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
        }
    }
}
