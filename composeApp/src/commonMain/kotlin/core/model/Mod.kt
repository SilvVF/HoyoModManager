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

    @ColumnInfo(name = "preview_images")
    val previewImages: List<String> = emptyList(),

    @ColumnInfo(name = "gb_id")
    val gbId: Int? = null,

    @ColumnInfo(name = "mod_link")
    val modLink: String? = null,

    @ColumnInfo(name = "gb_file_name")
    val gbFileName: String? = null,

    @ColumnInfo(name = "gb_download_link")
    val gbDownloadLink: String? = null,

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

