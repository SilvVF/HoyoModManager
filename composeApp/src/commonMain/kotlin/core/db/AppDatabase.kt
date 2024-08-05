package core.db

import OS
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import core.model.Character
import core.model.Game
import core.model.MetaData
import core.model.Mod
import core.model.Playlist
import core.model.PlaylistModCrossRef
import core.model.Tag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.coroutines.CoroutineContext

@Database(
    entities = [
        Mod::class,
        MetaData::class,
        Character::class,
        Tag::class,
        Playlist::class,
        PlaylistModCrossRef::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class InternalDatabase : RoomDatabase() {
    abstract val dao: DatabaseDao
}

class AppDatabase private constructor(
    private val delegate: InternalDatabase
) : DatabaseDao by delegate.dao {

    private val queryExecutor: CoroutineContext = Dispatchers.IO

    fun launchQuery(scope: CoroutineScope, block: suspend AppDatabase.() -> Unit) = with(delegate) {
        scope.launch(queryExecutor) {
            block(this@AppDatabase)
        }
    }

    fun launchQuery(block: suspend AppDatabase.() -> Unit) = with(delegate) {
        CoroutineScope(queryExecutor).launch {
            block(this@AppDatabase)
        }
    }

    suspend fun query(block: suspend AppDatabase.() -> Unit) = with(delegate) {
        withContext(queryExecutor) {
            block(this@AppDatabase)
        }
    }

    fun close() = delegate.close()

    companion object {

        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            AppDatabase(getRoomDatabase(getDatabaseBuilder()))
        }

        private fun getDatabaseBuilder(): RoomDatabase.Builder<InternalDatabase> {
            val dbFile = File(OS.getCacheDir(), "hmm.db")

            return Room.databaseBuilder<InternalDatabase>(
                name = dbFile.absolutePath,
            )
        }

        private fun getRoomDatabase(
            builder: RoomDatabase.Builder<InternalDatabase>
        ): InternalDatabase {
            return builder
                .setDriver(BundledSQLiteDriver())
                .fallbackToDestructiveMigration(true)
                .fallbackToDestructiveMigrationOnDowngrade(true)
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        }
    }
}


val LocalDatabase = staticCompositionLocalOf<AppDatabase> { error("Not provided") }


