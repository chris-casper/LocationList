// app/src/main/java/im/casper/locationlist/data/Location.kt
package im.casper.locationlist.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locations")
data class Location(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val latitude: Double?,
    val longitude: Double?,
    val description: String,
    val notes: String,
    val groups: List<String>,
    val tags: List<String>,
    val photoUris: List<String>,
    val createdAt: Long = System.currentTimeMillis(),
)