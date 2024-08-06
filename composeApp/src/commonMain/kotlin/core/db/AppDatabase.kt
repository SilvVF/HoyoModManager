package core.db

import OS
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import core.model.Character
import core.model.MetaData
import core.model.Mod
import core.model.Playlist
import core.model.PlaylistModCrossRef
import core.model.Tag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private val databaseScope =  CoroutineScope(queryExecutor + SupervisorJob())

    fun launchQuery(scope: CoroutineScope, block: suspend AppDatabase.() -> Unit) =
        scope.launch(queryExecutor) { block(this@AppDatabase) }

    fun launchQuery(block: suspend AppDatabase.() -> Unit) =
        databaseScope.launch { block(this@AppDatabase) }

    suspend fun query(block: suspend AppDatabase.() -> Unit) =
        withContext(queryExecutor) { block(this@AppDatabase) }

    suspend fun <T> execute(block: suspend AppDatabase.() -> T): T {
        return withContext(queryExecutor) { block(this@AppDatabase) }
    }

    fun close() = delegate.close()

    companion object {

        private const val DB_NAME = "hmm.db"

        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            AppDatabase(getRoomDatabase(getDatabaseBuilder()))
        }

        private fun getDatabaseBuilder(): RoomDatabase.Builder<InternalDatabase> {
            val dbFile = File(OS.getCacheDir(), DB_NAME)

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


