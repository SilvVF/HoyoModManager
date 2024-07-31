package core.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.util.performInTransactionSuspending
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.coroutines.coroutineContext

@Database(
    entities = [
        Mod::class,
        MetaData::class,
        Character::class,
        Tag::class,
        Playlist::class,
        PlaylistModCrossRef::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun modDao(): ModDao
    abstract fun prefsDao(): PrefsDao
    abstract fun characterDao(): CharacterDao
    abstract fun tagDao(): TagDao
    abstract fun playlistDao(): PlaylistDao
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

object DB {

    private val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        getRoomDatabase(getDatabaseBuilder())
    }

    val modDao by lazy { instance.modDao() }

    val prefsDao by lazy { instance.prefsDao() }

    val characterDao by lazy { instance.characterDao() }

    val tagDao by lazy { instance.tagDao() }

    val playlistDao by lazy { instance.playlistDao() }

    private fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
        val dbFile = File(OS.getCacheDir(), "hmm.db")

        return Room.databaseBuilder<AppDatabase>(
            name = dbFile.absolutePath,
        )
    }

    private fun getRoomDatabase(
        builder: RoomDatabase.Builder<AppDatabase>
    ): AppDatabase {
        return builder
            .setDriver(BundledSQLiteDriver())
            .fallbackToDestructiveMigration(true)
            .fallbackToDestructiveMigrationOnDowngrade(true)
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }
}

