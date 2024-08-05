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
