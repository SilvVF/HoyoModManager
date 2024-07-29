package core.model

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import core.db.ModEntity

@Entity(
    primaryKeys = ["file_name", "name"],
    foreignKeys = [
        ForeignKey(
            entity = ModEntity::class,
            parentColumns = arrayOf("file_name"),
            childColumns = arrayOf("file_name"),
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Tag(
    @ColumnInfo("file_name", index = true)
    val fileName: String,

    val name: String,
)

@Dao
interface TagDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: Tag)

    @Delete
    suspend fun delete(tag: Tag)
}
