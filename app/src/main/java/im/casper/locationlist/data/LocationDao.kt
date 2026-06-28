// app/src/main/java/im/casper/locationlist/data/LocationDao.kt
package im.casper.locationlist.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import androidx.room.Update

@Dao
interface LocationDao {
    @Insert
    suspend fun insert(location: Location): Long

    @Update
    suspend fun update(location: Location)

    @Query("SELECT * FROM locations ORDER BY createdAt DESC")
    fun getAll(): Flow<List<Location>>

    @Query("SELECT * FROM locations WHERE id = :id")
    suspend fun getById(id: Long): Location?

    @Query("DELETE FROM locations")
    suspend fun deleteAll()

    @Query("SELECT * FROM locations")
    suspend fun getAllOnce(): List<Location>
    @Delete
    suspend fun delete(location: Location)
}