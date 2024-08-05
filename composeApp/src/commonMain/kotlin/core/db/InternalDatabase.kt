package core.db

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transactor
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.immediateTransaction
import androidx.room.useReaderConnection
import androidx.room.useWriterConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import core.model.Character
import core.model.CharacterDao
import core.model.Game
import core.model.MetaData
import core.model.Mod
import core.model.ModDao
import core.model.Playlist
import core.model.PlaylistDao
import core.model.PlaylistModCrossRef
import core.model.PrefsDao
import core.model.Tag
import core.model.TagDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.xml.crypto.Data
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

class AppDatabase(
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

private const val KEY_VALUE_SEPARATOR = "->"
private const val ENTRY_SEPARATOR = "||"

class Converters {
    /**
     * return key1->value1||key2->value2||key3->value3
     */
    @TypeConverter
    fun mapToString(map: Map<Byte, String>): String {
        return try {
            map.entries.joinToString(separator = ENTRY_SEPARATOR) {
                println(it.key.toString() + "encode")
                "${it.key}$KEY_VALUE_SEPARATOR${it.value}"
            }
        } catch (e: Exception) {
            return ""
        }
    }

    /**
     * return map of String, String
     *        "key1": "value1"
     *        "key2": "value2"
     *        "key3": "value3"
     */
    @TypeConverter
    fun stringToMap(string: String): Map<Byte, String> {
        return try {
            string.split(ENTRY_SEPARATOR).associate {
                val (key, value) = it.split(KEY_VALUE_SEPARATOR)
                (key.toByte()) to value
            }
        } catch (e: Exception) {
            return emptyMap()
        }
    }

    @TypeConverter
    fun byteToGame(byte: Byte): Game {
        return Game.entries.first { it.data == byte }
    }

    @TypeConverter
    fun gameToByte(game: Game): Byte {
        return game.data
    }

    @TypeConverter
    fun listToString(list: List<String>): String {
        return Json.encodeToString(list)
    }

    @TypeConverter
    fun stringFromList(string: String): List<String> {
        return Json.decodeFromString(string)
    }
}

val LocalDatabase = staticCompositionLocalOf<AppDatabase> { error("Not provided") }


