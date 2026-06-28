// app/src/main/java/im/casper/locationlist/data/Converters.kt
package im.casper.locationlist.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String = value.joinToString("\n")

    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split("\n")
}