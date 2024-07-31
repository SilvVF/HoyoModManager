package core.model

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

private const val PREF_ID = 0


@Entity
data class MetaData(
    val exportModDir: Map<Byte, String>?,
    @PrimaryKey val id: Int = PREF_ID,

    val keepFilesOnClear: List<String> = emptyList()
)

@Dao
interface PrefsDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(data: MetaData)

    @Update
    suspend fun update(data: MetaData)

    @Transaction
    suspend fun addIgnoredFolder(id: Int = PREF_ID, path: String) {
        val curr = select()
        if (curr != null) {
            update(curr.copy(keepFilesOnClear = curr.keepFilesOnClear + path))
        } else {
            insert(MetaData(emptyMap(), PREF_ID, listOf(path)))
        }
    }

    @Transaction
    suspend fun removeIgnoredFolder(id: Int = PREF_ID, path: String) {
        val curr = select()
        if (curr != null) {
            update(curr.copy(keepFilesOnClear = curr.keepFilesOnClear - path))
        } else {
            insert(MetaData(emptyMap(), PREF_ID, emptyList()))
        }
    }

    @Query("SELECT * FROM MetaData WHERE id = 0 LIMIT 1")
    suspend fun select(): MetaData?

    @Query("SELECT * FROM MetaData WHERE id = 0 LIMIT 1")
    fun observe(): Flow<MetaData?>
}