// app/src/main/java/im/casper/locationlist/ui/screens/LocationDetailScreen.kt
package im.casper.locationlist.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import im.casper.locationlist.data.AppDatabase
import im.casper.locationlist.data.Location
import im.casper.locationlist.util.ImageStorage
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LocationDetailScreen(locationId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { AppDatabase.get(context).locationDao() }

    // The original record — kept so we preserve its id and createdAt on save.
    var original by remember { mutableStateOf<Location?>(null) }
    var loaded by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var groups by remember { mutableStateOf<List<String>>(emptyList()) }
    var tags by remember { mutableStateOf<List<String>>(emptyList()) }
    var photoPaths by remember { mutableStateOf<List<String>>(emptyList()) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Load the record once, then populate the editable fields.
    LaunchedEffect(locationId) {
        val loc = dao.getById(locationId)
        if (loc != null) {
            original = loc
            name = loc.name
            description = loc.description
            notes = loc.notes
            latitude = loc.latitude?.toString() ?: ""
            longitude = loc.longitude?.toString() ?: ""
            groups = loc.groups
            tags = loc.tags
            photoPaths = loc.photoUris
        }
        loaded = true
    }

    // Camera capture
    var pendingPhoto by remember { mutableStateOf<File?>(null) }
    val takePicture = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) pendingPhoto?.let { photoPaths = photoPaths + it.absolutePath }
        pendingPhoto = null
    }

    // Gallery picker
    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) photoPaths = photoPaths + ImageStorage.importImage(context, uri)
    }

    fun openInMaps() {
        val lat = latitude.toDoubleOrNull()
        val lng = longitude.toDoubleOrNull()
        if (lat == null || lng == null) {
            Toast.makeText(context, "No coordinates to map", Toast.LENGTH_SHORT).show()
            return
        }
        val label = Uri.encode(name.ifBlank { "Location" })
        // geo: lets the user's default map / nav app handle it (chooser if several).
        val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($label)")
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: Exception) {
            Toast.makeText(context, "No map app found on this device", Toast.LENGTH_SHORT).show()
        }
    }

    fun save() {
        val current = original ?: return
        scope.launch {
            dao.update(
                current.copy(
                    name = name.trim(),
                    latitude = latitude.toDoubleOrNull(),
                    longitude = longitude.toDoubleOrNull(),
                    description = description.trim(),
                    notes = notes.trim(),
                    groups = groups,
                    tags = tags,
                    photoUris = photoPaths,
                )
            )
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit location") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (original != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                },
            )
        }
    ) { padding ->
        when {
            !loaded -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) { Text("Loading…") }
            }
            original == null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) { Text("This location no longer exists.") }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        label = { Text("Name") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = latitude, onValueChange = { latitude = it },
                            label = { Text("Latitude") }, singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = longitude, onValueChange = { longitude = it },
                            label = { Text("Longitude") }, singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    FilledTonalButton(
                        onClick = { openInMaps() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Place, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Open in Maps")
                    }

                    OutlinedTextField(
                        value = description, onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = notes, onValueChange = { notes = it },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                    )

                    ChipInput("Groups", groups) { groups = it }
                    ChipInput("Tags", tags) { tags = it }

                    Text("Photos", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            val file = ImageStorage.newImageFile(context)
                            pendingPhoto = file
                            takePicture.launch(ImageStorage.uriFor(context, file))
                        }) { Text("Take photo") }
                        OutlinedButton(onClick = {
                            pickImage.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }) { Text("From gallery") }
                    }
                    if (photoPaths.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(photoPaths) { path ->
                                Box {
                                    AsyncImage(
                                        model = File(path),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(120.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                    )
                                    IconButton(
                                        onClick = { photoPaths = photoPaths - path },
                                        modifier = Modifier.align(Alignment.TopEnd),
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove photo")
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { save() },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Save changes") }
                }
            }
        }
    }

    if (showDeleteDialog) {
        val loc = original
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete location?") },
            text = { Text("\"${loc?.name.orEmpty()}\" will be permanently removed.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    if (loc != null) scope.launch { dao.delete(loc); onBack() }
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ChipInput(
    label: String,
    items: List<String>,
    onItemsChange: (List<String>) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = text, onValueChange = { text = it },
                label = { Text(label) }, singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = {
                if (text.isNotBlank()) { onItemsChange(items + text.trim()); text = "" }
            }) { Text("Add") }
        }
        if (items.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEach { item ->
                    InputChip(
                        selected = false,
                        onClick = { onItemsChange(items - item) },
                        label = { Text(item) },
                        trailingIcon = {
                            Icon(Icons.Default.Close, contentDescription = "Remove")
                        },
                    )
                }
            }
        }
    }
}
