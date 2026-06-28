// app/src/main/java/im/casper/locationlist/ui/home/HomeScreen.kt
package im.casper.locationlist.ui.home

import im.casper.locationlist.navigation.Routes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

private data class HomeMenuItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    val menuItems = listOf(
        HomeMenuItem("Locations", "Browse and search everything you've saved",
            Icons.AutoMirrored.Filled.List, Routes.LOCATION_LIST),
        HomeMenuItem("Add a location", "Save a place with notes, tags, and a photo",
            Icons.Default.Add, Routes.CREATE_LOCATION),
        HomeMenuItem("Map", "See all your locations on a map",
            Icons.Default.Place, Routes.MAP),
        HomeMenuItem("Share / Export", "Send locations or back them up to a file",
            Icons.Default.Share, Routes.SHARE),
        HomeMenuItem("Settings", "Preferences, import, and about",
            Icons.Default.Settings, Routes.SETTINGS),
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text("LocationList") }) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(menuItems) { item ->
                HomeMenuCard(item = item, onClick = { onNavigate(item.route) })
            }
        }
    }
}

@Composable
private fun HomeMenuCard(item: HomeMenuItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    item.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}