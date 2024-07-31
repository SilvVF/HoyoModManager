package core.model

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "mod",
    indices = [Index("file_name", "character_id", unique = true)]
)
data class Mod(

    @ColumnInfo(name = "file_name")
    val fileName: String,

    @ColumnInfo(name = "game", index = true)
    val game: Byte,

    @ColumnInfo(name = "character")
    val character: String,

    @ColumnInfo(name = "character_id")
    val characterId: Int,

    @ColumnInfo(name = "enabled", index = true)
    val enabled: Boolean,

    @ColumnInfo(name = "mod_link")
    val modLink: String? = null,

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0
)

data class ModWithTags(

    @Embedded
    val mod: Mod,

    @Relation(
        parentColumn = "id",
        entityColumn = "mod_id"
    )
    val tags: List<Tag>
)

@Dao
interface ModDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mod: Mod)

    @Delete
    suspend fun delete(mod: Mod)

    @Query("DELETE FROM mod WHERE file_name NOT IN (:used) AND game = :game")
    suspend fun deleteUnused(used: List<String>, game: Byte)

    @Query("DELETE FROM mod WHERE game = :game")
    suspend fun clear(game: Byte)

    @Query("SELECT * FROM mod WHERE game = :game")
    fun observeByGame(game: Byte): Flow<List<Mod>>

    @Query("SELECT file_name FROM mod WHERE game = :game")
    fun observeFileNamesByGame(game: Byte): Flow<List<String>>

    @Query("SELECT * FROM mod WHERE file_name = :name LIMIT 1")
    fun observeByFileName(name: String): Flow<Mod?>

    @Query("SELECT * FROM mod WHERE game = :game")
    suspend fun selectAllByGame(game: Byte): List<Mod>

    @Query("SELECT * FROM mod WHERE file_name = :name LIMIT 1")
    suspend fun selectByFileName(name: String): Mod?

    @Query("SELECT * FROM mod WHERE id = :id LIMIT 1")
    suspend fun selectById(id: Int): Mod?

    @Query("SELECT COUNT(*) FROM mod WHERE character = :name")
    fun observeCountByCharacter(name: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM mod WHERE character = :name AND enabled")
    fun observeEnabledCountByCharacter(name: String): Flow<Int>

    @Query("SELECT * FROM mod WHERE character = :name")
    fun observeByCharacter(name: String): Flow<List<Mod>>

    @Update
    suspend fun update(mod: Mod)

    @Transaction
    @Query("""
        update mod SET 
            enabled = CASE WHEN id in (:enabled) THEN true ELSE FALSE
             END
        WHERE game = :game
    """)
    suspend fun enableAndDisable(enabled: List<Int>, game: Game)

    @Query("SELECT * FROM mod WHERE file_name = :fileName AND character = :character LIMIT 1")
    suspend fun selectByFileNameAndCharacter(fileName: String, character: String): Mod?

    @Transaction
    suspend fun insertOrUpdate(mod: Mod) {
        val existing = selectByFileNameAndCharacter(mod.fileName, mod.character)
        if (existing != null) {
            update(mod)
        } else {
            insert(mod)
        }
    }

    @Query("""
        SELECT * FROM mod WHERE enabled AND game = :game 
    """)
    suspend fun selectEnabledForGame(game: Byte): List<Mod>

    @Transaction
    @Query("SELECT * FROM mod WHERE character = :character AND game = :game")
    fun observeModsWithTags(character: String, game: Byte): Flow<List<ModWithTags>>

    @Transaction
    @Query("SELECT * FROM mod WHERE game = :game")
    fun observeAllModsWithTags(game: Byte): Flow<List<ModWithTags>>
}