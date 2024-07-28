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

@Entity(tableName = "mod")
data class ModEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "game", index = true) val game: Byte,
    @ColumnInfo(name = "character") val character: String,
    @ColumnInfo(name = "file_name", index = true) val fileName: String,
    @ColumnInfo(name = "enabled") val enabled: Boolean
)

data class ModUpdate(
    val id: Int,
    val character: String? = null,
    val fileName: String? = null,
    val enabled: Boolean? = null
)

@Dao
interface ModDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(mod: ModEntity)

    @Delete
    suspend fun delete(mod: ModEntity)

    @Query("SELECT * FROM mod WHERE game = :game")
    suspend fun selectAllByGame(game: Byte): List<ModEntity>

    @Query("SELECT * FROM mod WHERE file_name = :name LIMIT 1")
    suspend fun selectByFileName(name: String): ModEntity?

    @Query("""
        UPDATE MOD SET 
            character = COALESCE(character, :character), 
            file_name = COALESCE(file_name, :fileName), 
            enabled = COALESCE(enabled, :enabled)
        WHERE id = :id
    """)
    suspend fun update(id: Int, character: String?, fileName: String?, enabled: Boolean?)
}