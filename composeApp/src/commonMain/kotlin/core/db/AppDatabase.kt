package core.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.ktor.util.decodeBase64String
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.TreeMap

@Database(
    entities = [ModEntity::class, MetaData::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun modDao(): ModDao
    abstract fun prefsDao(): PrefsDao
}

private const val KEY_VALUE_SEPARATOR = "->"
private const val ENTRY_SEPARATOR = "||"

class Converters {
    /**
     * return key1->value1||key2->value2||key3->value3
     */
    @OptIn(ExperimentalStdlibApi::class)
    @TypeConverter
    fun mapToString(map: Map<Byte, String>): String {
        return map.entries.joinToString(separator = ENTRY_SEPARATOR) {
            println(it.key.toString() + "encode")
            "${it.key}$KEY_VALUE_SEPARATOR${it.value}"
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
        return string.split(ENTRY_SEPARATOR).associate {
            val (key, value) = it.split(KEY_VALUE_SEPARATOR)
            (key.toByte()) to value
        }
    }
}

object DB {

    private val instance by lazy { getRoomDatabase(getDatabaseBuilder()) }

    val modDao by lazy { instance.modDao() }

    val prefsDao by lazy { instance.prefsDao() }

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
