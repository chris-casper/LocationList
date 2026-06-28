// app/src/main/java/im/casper/locationlist/ui/screens/MapScreen.kt
package im.casper.locationlist.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import im.casper.locationlist.data.AppDatabase
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(onBack: () -> Unit, onLocationClick: (Long) -> Unit) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).locationDao() }
    val locations by dao.getAll().collectAsState(initial = emptyList())

    var hasCentered by remember { mutableStateOf(false) }
    val hasAnyCoords = locations.any { it.latitude != null && it.longitude != null }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Map") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    Configuration.getInstance().load(
                        ctx,
                        ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE),
                    )
                    Configuration.getInstance().userAgentValue = ctx.packageName

                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(4.0)
                        controller.setCenter(GeoPoint(20.0, 0.0))
                        onResume()
                    }
                },
                update = { mapView ->
                    mapView.overlays.clear()

                    // Standard osmdroid pin for every marker.
                    val pin = ContextCompat.getDrawable(
                        mapView.context,
                        im.casper.locationlist.R.drawable.locationlist_pin_small,
                    )

                    val points = locations.mapNotNull { loc ->
                        val lat = loc.latitude
                        val lng = loc.longitude
                        if (lat != null && lng != null) {
                            Triple(loc.id, GeoPoint(lat, lng), loc.name)
                        } else {
                            null
                        }
                    }

                    points.forEach { (id, geo, title) ->
                        val marker = Marker(mapView)
                        marker.position = geo
                        marker.title = title
                        marker.icon = pin
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        marker.setOnMarkerClickListener { _, _ ->
                            onLocationClick(id)
                            true
                        }
                        mapView.overlays.add(marker)
                    }

                    if (!hasCentered && points.isNotEmpty()) {
                        mapView.controller.setZoom(12.0)
                        mapView.controller.setCenter(points.first().second)
                        hasCentered = true
                    }

                    mapView.invalidate()
                },
                onRelease = { mapView ->
                    mapView.onPause()
                    mapView.onDetach()
                },
            )

            if (!hasAnyCoords) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp),
                ) {
                    Text(
                        "No saved locations have coordinates yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}
