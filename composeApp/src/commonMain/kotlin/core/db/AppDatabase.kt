package core.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import java.io.File

@Database(
    entities = [ModEntity::class, MetaData::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun modDao(): ModDao
    abstract fun prefsDao(): PrefsDao
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
