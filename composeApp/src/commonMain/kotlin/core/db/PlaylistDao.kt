package core.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import androidx.room.Update
import core.model.Game
import core.model.Mod
import core.model.Playlist
import core.model.PlaylistModCrossRef
import core.model.PlaylistWithMods
import core.model.Tag
import kotlinx.coroutines.flow.Flow

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
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Update
    suspend fun updatePlaylist(playlist: Playlist)

    @Delete
    suspend fun deletePlaylist(playlist: Playlist)

    @Insert
    suspend fun insertPlaylist(playlistModCrossRef: PlaylistModCrossRef)

    @Insert
    suspend fun insertAll(playlistModCrossRef: List<PlaylistModCrossRef>)

}