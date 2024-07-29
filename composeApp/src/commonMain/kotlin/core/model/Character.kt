package core.model

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Entity(tableName = "character", primaryKeys = ["id", "game"])
data class Character(
    val id: Int,
    val game: Game,
    val name: String,
    @ColumnInfo("avatar_url")
    val avatarUrl: String,
    val element: String,
)

@Dao
interface CharacterDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(character: Character)

    @Update
    suspend fun update(character: Character)

    @Query("DELETE FROM character WHERE id = :id AND game = :game")
    suspend fun delete(id: Int, game: Game)

    @Query("SELECT * FROM character WHERE game = :game")
    suspend fun selectByGame(game: Game): List<Character>
}