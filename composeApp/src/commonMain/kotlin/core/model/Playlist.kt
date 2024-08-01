package core.model

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.RoomWarnings
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

@Dao
interface PlaylistDao {

    @Transaction
    @Query("SELECT * FROM playlist WHERE game = :game")
    fun subscribeToPlaylistsWithMods(game: Game): Flow<List<PlaylistWithMods>>

    @RewriteQueriesToDropUnusedColumns
    @Transaction
    @Query("""
        SELECT 
            p.*,
            m.*,
            t.*
        FROM 
            playlist p
        JOIN 
            playlistmodcrossref pmcr ON p.playlistId = pmcr.playlistId
        JOIN 
            mod m ON pmcr.id = m.id
        LEFT JOIN 
            tag t ON m.id = t.mod_id
        WHERE p.game = :game
        ORDER BY 
            p.playlistId, m.id, t.name
    """)
    fun subscribeToPlaylistsWithModsAndTags(game: Game): Flow<Map<Playlist, Map<Mod, List<Tag>>>>


    @Insert
    suspend fun insert(playlist: Playlist): Long

    @Update
    suspend fun update(playlist: Playlist)

    @Delete
    suspend fun delete(playlist: Playlist)

    @Insert
    suspend fun insert(playlistModCrossRef: PlaylistModCrossRef)

    @Insert
    suspend fun insert(playlistModCrossRef: List<PlaylistModCrossRef>)

}