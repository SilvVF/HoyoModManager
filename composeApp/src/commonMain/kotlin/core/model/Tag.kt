package core.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
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

