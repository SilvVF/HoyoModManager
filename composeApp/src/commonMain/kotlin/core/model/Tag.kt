package core.model

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.OnConflictStrategy

@Entity(
    primaryKeys = ["mod_id", "name"],
    foreignKeys = [
        ForeignKey(
            entity = Mod::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("mod_id"),
            onDelete = ForeignKey.CASCADE,
        )
    ]
)
data class Tag(

    @ColumnInfo("mod_id")
    val modId: Int,

    val name: String,
)
