package core.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "mod")
data class ModEntity(
    @PrimaryKey
    @ColumnInfo(name = "file_name")
    val fileName: String,
    @ColumnInfo(name = "id") val id: Int,
    @ColumnInfo(name = "game", index = true) val game: Byte,
    @ColumnInfo(name = "character") val character: String,
    @ColumnInfo(name = "enabled", index = true) val enabled: Boolean
)

data class ModUpdate(
    val id: Int,
    val character: String? = null,
    val fileName: String? = null,
    val enabled: Boolean? = null
)

@Dao
interface ModDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mod: ModEntity)

    @Delete
    suspend fun delete(mod: ModEntity)

    @Query("DELETE FROM mod")
    suspend fun clear()

    @Query("SELECT * FROM mod WHERE game = :game")
    fun observeByGame(game: Byte): Flow<List<ModEntity>>

    @Query("SELECT * FROM mod WHERE file_name = :name LIMIT 1")
    fun observeByFileName(name: String): Flow<ModEntity?>

    @Query("SELECT * FROM mod WHERE game = :game")
    suspend fun selectAllByGame(game: Byte): List<ModEntity>

    @Query("SELECT * FROM mod WHERE file_name = :name LIMIT 1")
    suspend fun selectByFileName(name: String): ModEntity?

    @Query("SELECT COUNT(file_name) FROM mod WHERE character = :name")
    fun observeCountByCharacter(name: String): Flow<Int>

    @Query("SELECT COUNT(file_name) FROM mod WHERE character = :name AND enabled")
    fun observeEnabledCountByCharacter(name: String): Flow<Int>

    @Query("SELECT * FROM mod WHERE character = :name")
    fun observeByCharacter(name: String): Flow<List<ModEntity?>>

    @Update
    suspend fun update(mod: ModEntity)

    @Query("""
        SELECT * FROM mod WHERE enabled AND game = :game 
    """)
    suspend fun selectEnabledForGame(game: Byte): List<ModEntity>
}