package core.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

private const val PREF_ID = 0

@Entity
data class MetaData(
    val exportModDir: String?,
    @PrimaryKey val id: Int = PREF_ID,
)

@Dao
interface PrefsDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(data: MetaData)

    @Update
    suspend fun update(data: MetaData)

    @Query("SELECT * FROM MetaData WHERE id = 0 LIMIT 1")
    suspend fun select(): MetaData?

    @Query("SELECT * FROM MetaData WHERE id = 0 LIMIT 1")
    fun observe(): Flow<MetaData?>
}