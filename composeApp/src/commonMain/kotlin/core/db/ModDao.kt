package core.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import core.model.Game
import core.model.Mod
import core.model.ModWithTags
import kotlinx.coroutines.flow.Flow

@Dao
interface ModDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMod(mod: Mod)

    @Delete
    suspend fun deleteMod(mod: Mod)

    @Query("DELETE FROM mod WHERE file_name NOT IN (:used) AND game = :game")
    suspend fun deleteUnusedMods(used: List<String>, game: Byte)

    @Query("DELETE FROM mod WHERE game = :game")
    suspend fun deleteModsByGame(game: Byte)

    @Query("SELECT * FROM mod WHERE game = :game")
    fun subscribeToMod(game: Byte): Flow<List<Mod>>

    @Query("SELECT file_name FROM mod WHERE game = :game")
    fun subscribeToModFilenames(game: Byte): Flow<List<String>>

    @Query("SELECT * FROM mod WHERE file_name = :name LIMIT 1")
    fun subscribeToMod(name: String): Flow<Mod?>

    @Query("SELECT * FROM mod WHERE game = :game")
    suspend fun selectModsByGame(game: Byte): List<Mod>

    @Query("SELECT * FROM mod WHERE file_name = :name LIMIT 1")
    suspend fun selectModByFilename(name: String): Mod?

    @Query("SELECT * FROM mod WHERE id = :id LIMIT 1")
    suspend fun selectModById(id: Int): Mod?

    @Query("SELECT COUNT(*) FROM mod WHERE character = :characterName")
    fun subscribeToModCount(characterName: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM mod WHERE character = :characterName AND enabled")
    fun subscribeToEnabledModCount(characterName: String): Flow<Int>

    @Query("SELECT * FROM mod WHERE character = :characterName")
    fun subscribeToMods(characterName: String): Flow<List<Mod>>

    @Query("SELECT * FROM mod WHERE gb_id = :gameBananaId")
    fun subscribeToModByGbId(gameBananaId: Int): Flow<List<Mod>>

    @Query(
        """
            SELECT 
                COUNT(DISTINCT m.id)
            FROM 
                mod m
            WHERE 
                m.game = :game AND (
                    m.file_name LIKE '%' + :search + '%' OR
                    m.gb_file_name LIKE '%' + :search + '%'
                )
        """
    )
    suspend fun selectCountModsContaining(search: String, game: Game): Int

    @Update
    suspend fun updateMod(mod: Mod)

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
            updateMod(mod)
        } else {
            insertMod(mod)
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