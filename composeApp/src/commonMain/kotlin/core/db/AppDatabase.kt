package core.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import java.io.File

@Database(
    entities = [ModEntity::class, Prefs::class],
    version = 2
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun modDao(): ModDao
    abstract fun prefsDao(): PrefsDao
}

object DB {

    private val instance by lazy { getRoomDatabase(getDatabaseBuilder()) }

    val GENSHIN: Byte = 0x01
    val STAR_RAIL: Byte = 0x02
    val ZZZ: Byte = 0x03

    val modDao by lazy { instance.modDao() }

    val prefsDao by lazy { instance.prefsDao() }

    private fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
        val dbFile = File(System.getProperty("java.io.tmpdir"), "hmm.db")
        return Room.databaseBuilder<AppDatabase>(
            name = dbFile.absolutePath,
        )
    }

    private fun getRoomDatabase(
        builder: RoomDatabase.Builder<AppDatabase>
    ): AppDatabase {
        return builder
            .fallbackToDestructiveMigrationOnDowngrade(true)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }

}
