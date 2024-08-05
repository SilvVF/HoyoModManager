package core.model

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity
data class Playlist(
    @PrimaryKey(autoGenerate = true)
    val playlistId: Int = 0,
    val name: String,
    val game: Game
)

@Entity(
    primaryKeys = ["playlistId", "id"],
    foreignKeys = [
        ForeignKey(
            entity = Mod::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("id"),
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Playlist::class,
            parentColumns = arrayOf("playlistId"),
            childColumns = arrayOf("playlistId"),
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PlaylistModCrossRef(
    @ColumnInfo(index = true)
    val playlistId: Int,
    @ColumnInfo(index = true)
    val id: Int,
)

data class PlaylistWithModsAndTags(
    val playlist: Playlist,
    val mods: List<ModWithTags>
)


data class PlaylistWithMods(

    @Embedded
    val playlist: Playlist,

    @Relation(
        parentColumn = "playlistId",
        entityColumn = "id",
        associateBy = Junction(
            value = PlaylistModCrossRef::class,
            parentColumn = "playlistId",
            entityColumn = "id"
        )
    )
    val mods: List<Mod>
)
