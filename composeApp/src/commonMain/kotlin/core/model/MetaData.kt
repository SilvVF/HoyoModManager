package core.model

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

internal const val PREF_ID = 0

@Entity
data class MetaData(
    val exportModDir: Map<Byte, String>?,
    @PrimaryKey val id: Int = PREF_ID,

    val keepFilesOnClear: List<String> = emptyList()
)
