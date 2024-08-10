package core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import core.model.Character
import core.model.Game
import core.model.Mod
import core.model.Tag
import kotlinx.coroutines.flow.Flow

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

    @Query(
        "SELECT * FROM character WHERE game = :game AND LOWER(name) LIKE '%' || LOWER(:search) || '%'"
    )
    suspend fun selectCharactersNamesContaining(search: String, game: Game): List<Character>

    @Query("SELECT * FROM character WHERE LOWER(name) LIKE '%' || LOWER(:name) || '%' AND game = :game LIMIT 1")
    suspend fun selectClosestMatch(game: Game, name: String): Character?

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
        AND ((
            m.file_name LIKE '%' || :modFileName || '%'
            OR c.name LIKE '%' || :characterName || '%'
            OR t.name LIKE '%' || :tagName || '%'
        ) OR (:modFileName is NULL AND :characterName is NULL AND :tagName is NULL ))
        ORDER BY 
            c.name, m.file_name, t.name
    """)
    fun observeByGameWithMods(game: Game, modFileName: String?, characterName: String?, tagName: String?): Flow<Map<Character, Map<Mod, List<Tag>>>>
}