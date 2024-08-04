package core.model

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "character", primaryKeys = ["id", "game"])
data class Character(
    val id: Int,
    val game: Game,
    val name: String,
    @ColumnInfo("avatar_url")
    val avatarUrl: String,
    val element: String,
)

data class CharacterWithMods(
    @Embedded
    val character: Character,

    @Relation(
        entity = Character::class,
        parentColumn = "id",
        entityColumn = "characterId",
    )
    val mods: List<Mod>
)

data class CharacterWithModsAndTags(
    val character: Character,
    val mods: List<ModWithTags>
)

@Dao
interface CharacterDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(character: Character)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(character: List<Character>)

    @Update
    suspend fun update(character: Character)

    @Query("SELECT * FROM character WHERE id = :id AND game = :game LIMIT 1")
    suspend fun selectById(id: Int, game: Game): Character?

    @Query("SELECT * FROM character WHERE :name LIKE name AND game = :game LIMIT 1")
    suspend fun selectClosesMatch(game: Game, name: String): Character?

    @Transaction
    suspend fun updateFromCharacters(characters: List<Character>) {
        for (character in characters) {
            val existing = selectById(character.id, character.game)
            if (existing != null)  {
                update(character)
            } else {
                insert(character)
            }
        }
        if (characters.isNotEmpty()) {
            selectByGame(characters.first().game)
                .filter { it !in characters }
                .onEach { delete(it.id, it.game) }
        }
    }

    @Query("DELETE FROM character WHERE id = :id AND game = :game")
    suspend fun delete(id: Int, game: Game)

    @Query("SELECT name FROM character WHERE game = :game")
    suspend fun selectNamesByGame(game: Game): List<String>

    @Query("SELECT * FROM character WHERE game = :game")
    suspend fun selectByGame(game: Game): List<Character>

    @Query("SELECT * FROM character WHERE game = :game")
    fun observeByGame(game: Game): Flow<List<Character>>

    @Transaction
    @Query("""
        SELECT 
            c.*,
            m.*,
            t.*
        FROM 
            character c
        LEFT JOIN 
            mod m ON m.character_id = c.id
        LEFT JOIN 
            tag t ON t.mod_id = m.id
        WHERE c.game = :game
        ORDER BY 
            c.name, m.file_name, t.name
    """)
    fun observeByGameWithMods(game: Game): Flow<Map<Character, Map<Mod, List<Tag>>>>
}