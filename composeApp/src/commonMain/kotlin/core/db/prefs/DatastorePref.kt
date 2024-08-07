package core.db.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking


interface PreferenceStore {

    fun getString(key: String, defaultValue: String = ""): Preference<String>

    fun getLong(key: String, defaultValue: Long = 0): Preference<Long>

    fun getInt(key: String, defaultValue: Int = 0): Preference<Int>

    fun getFloat(key: String, defaultValue: Float = 0f): Preference<Float>

    fun getBoolean(key: String, defaultValue: Boolean = false): Preference<Boolean>

    fun getStringSet(key: String, defaultValue: Set<String> = emptySet()): Preference<Set<String>>

    fun <T> getObject(
        key: String,
        defaultValue: T,
        serializer: (T) -> String,
        deserializer: (String) -> T,
    ): Preference<T>

    suspend fun getAll(): Map<Preferences.Key<*>, Any>
}

interface Preference<T> {

    fun key(): String

    suspend fun get(): T

    suspend fun set(value: T)

    suspend fun isSet(): Boolean

    suspend fun delete()

    fun defaultValue(): T

    fun changes(): Flow<T>

    fun stateIn(scope: CoroutineScope): StateFlow<T>

    companion object {
        /**
         * A preference that should not be exposed in places like backups without user consent.
         */
        fun isPrivate(key: String): Boolean {
            return key.startsWith(PRIVATE_PREFIX)
        }
        fun privateKey(key: String): String {
            return "$PRIVATE_PREFIX$key"
        }
        /**
         * A preference used for internal app state that isn't really a user preference
         * and therefore should not be in places like backups.
         */
        fun isAppState(key: String): Boolean {
            return key.startsWith(APP_STATE_PREFIX)
        }
        fun appStateKey(key: String): String {
            return "$APP_STATE_PREFIX$key"
        }

        private const val APP_STATE_PREFIX = "__APP_STATE_"
        private const val PRIVATE_PREFIX = "__PRIVATE_"
    }
}

inline fun <reified T : Enum<T>> PreferenceStore.getEnum(
    key: String,
    defaultValue: T,
): Preference<T> {
    return getObject(
        key = key,
        defaultValue = defaultValue,
        serializer = { it.name },
        deserializer = {
            try {
                enumValueOf(it)
            } catch (e: IllegalArgumentException) {
                defaultValue
            }
        }
    )
}

suspend inline fun <reified T, R : T> Preference<T>.getAndSet(crossinline block: (T) -> R) = set(
    block(get()),
)

suspend operator fun <T> Preference<Set<T>>.plusAssign(item: T) {
    set(get() + item)
}

suspend operator fun <T> Preference<Set<T>>.minusAssign(item: T) {
    set(get() - item)
}

suspend fun Preference<Boolean>.toggle(): Boolean {
    set(!get())
    return get()
}

/**
 * Modified from https://github.com/aniyomiorg/aniyomi AndroidPreference
 * to use datastore instead of SharedPreferences
 */
sealed class DataStorePreference<T>(
    protected val dataStore: DataStore<Preferences>,
    private val key: String,
    private val defaultValue: T,
) : Preference<T> {

    abstract suspend fun read(datastore: DataStore<Preferences>, key: String, defaultValue: T): T

    abstract suspend fun write(key: String, value: T)

    abstract fun changesFlow(datastore: DataStore<Preferences>, key: String, defaultValue: T): Flow<T>

    abstract suspend fun delete(key: String)

    override fun key(): String { return key }

    override suspend fun get(): T {
        return try {
            read(dataStore, key, defaultValue)
        } catch (e: ClassCastException) {
            delete()
            defaultValue
        }
    }

    override suspend fun set(value: T) { write(key, value) }

    override suspend fun isSet(): Boolean {
        return dataStore.data
            .firstOrNull()
            ?.asMap()
            ?.any { entry -> entry.key.name == key }
            ?: false
    }

    override fun defaultValue(): T {
        return defaultValue
    }

    override suspend fun delete() { delete(key) }

    override fun changes(): Flow<T> {
        return changesFlow(dataStore, key, defaultValue).conflate()
    }

    override fun stateIn(scope: CoroutineScope): StateFlow<T> {
        return changes().stateIn(
            scope,
            SharingStarted.Eagerly,
            runBlocking { get() }
        )
    }

    class StringSetPrimitive(
        preferences: DataStore<Preferences>,
        key: String,
        defaultValue: Set<String>
    ) : DataStorePreference<Set<String>>(preferences, key, defaultValue) {
        override fun changesFlow(
            datastore: DataStore<Preferences>,
            key: String,
            defaultValue: Set<String>
        ): Flow<Set<String>> {
            return dataStore.data.map { it[stringSetPreferencesKey(key)] ?: defaultValue }
        }

        override suspend fun read(
            datastore: DataStore<Preferences>,
            key: String,
            defaultValue: Set<String>,
        ):  Set<String> {
            return dataStore.data.firstOrNull()?.get(stringSetPreferencesKey(key)) ?: defaultValue
        }

        override suspend fun delete(key: String) {
            dataStore.edit { it.remove(stringSetPreferencesKey(key)) }
        }
        override suspend fun write(key: String, value:  Set<String>) {
            dataStore.edit { it[stringSetPreferencesKey(key)] = value }
        }
    }

    class StringPrimitive(
        preferences: DataStore<Preferences>,
        key: String,
        defaultValue: String,
    ) : DataStorePreference<String>(preferences, key, defaultValue) {

        override fun changesFlow(
            datastore: DataStore<Preferences>,
            key: String,
            defaultValue: String
        ): Flow<String> {
            return dataStore.data.map { it[stringPreferencesKey(key)] ?: defaultValue }
        }

        override suspend fun read(
            datastore: DataStore<Preferences>,
            key: String,
            defaultValue: String,
        ):  String {
            return dataStore.data.firstOrNull()?.get(stringPreferencesKey(key)) ?: defaultValue
        }

        override suspend fun delete(key: String) {
            dataStore.edit { it.remove(stringPreferencesKey(key)) }
        }
        override suspend fun write(key: String, value:  String) {
            dataStore.edit { it[stringPreferencesKey(key)] = value }
        }
    }

    class LongPrimitive(
        preferences: DataStore<Preferences>,
        key: String,
        defaultValue: Long,
    ) : DataStorePreference<Long>(preferences, key, defaultValue) {
        override fun changesFlow(
            datastore: DataStore<Preferences>,
            key: String,
            defaultValue: Long
        ): Flow<Long> {
            return dataStore.data.map { it[longPreferencesKey(key)] ?: defaultValue }
        }

        override suspend fun read(
            datastore: DataStore<Preferences>,
            key: String,
            defaultValue: Long,
        ):  Long {
            return dataStore.data.firstOrNull()?.get(longPreferencesKey(key)) ?: defaultValue
        }

        override suspend fun delete(key: String) {
            dataStore.edit { it.remove(longPreferencesKey(key)) }
        }
        override suspend fun write(key: String, value:  Long) {
            dataStore.edit { it[longPreferencesKey(key)] = value }
        }
    }

    class IntPrimitive(
        preferences: DataStore<Preferences>,
        key: String,
        defaultValue: Int,
    ) : DataStorePreference<Int>(preferences, key, defaultValue) {
        override fun changesFlow(
            datastore: DataStore<Preferences>,
            key: String,
            defaultValue: Int
        ): Flow<Int> {
            return dataStore.data.map { it[intPreferencesKey(key)] ?: defaultValue }
        }

        override suspend fun read(
            datastore: DataStore<Preferences>,
            key: String,
            defaultValue: Int,
        ):  Int {
            return dataStore.data.firstOrNull()?.get(intPreferencesKey(key)) ?: defaultValue
        }

        override suspend fun delete(key: String) {
            dataStore.edit { it.remove(intPreferencesKey(key)) }
        }
        override suspend fun write(key: String, value:  Int) {
            dataStore.edit { it[intPreferencesKey(key)] = value }
        }
    }

    class FloatPrimitive(
        preferences: DataStore<Preferences>,
        key: String,
        defaultValue: Float,
    ) : DataStorePreference<Float>(preferences, key, defaultValue) {
        override fun changesFlow(
            datastore: DataStore<Preferences>,
            key: String,
            defaultValue: Float
        ): Flow<Float> {
            return dataStore.data.map { it[floatPreferencesKey(key)] ?: defaultValue }
        }

        override suspend fun read(
            datastore: DataStore<Preferences>,
            key: String,
            defaultValue: Float,
        ):  Float {
            return dataStore.data.firstOrNull()?.get(floatPreferencesKey(key)) ?: defaultValue
        }

        override suspend fun delete(key: String) {
            dataStore.edit { it.remove(floatPreferencesKey(key)) }
        }
        override suspend fun write(key: String, value:  Float) {
            dataStore.edit { it[floatPreferencesKey(key)] = value }
        }
    }

    class BooleanPrimitive(
        preferences: DataStore<Preferences>,
        key: String,
        defaultValue: Boolean,
    ) : DataStorePreference<Boolean>(preferences, key, defaultValue) {
        override fun changesFlow(
            datastore: DataStore<Preferences>,
            key: String,
            defaultValue: Boolean
        ): Flow<Boolean> {
            return dataStore.data.map { it[booleanPreferencesKey(key)] ?: defaultValue }
        }

        override suspend fun read(
            datastore: DataStore<Preferences>,
            key: String,
            defaultValue: Boolean,
        ):  Boolean {
            return dataStore.data.firstOrNull()?.get(booleanPreferencesKey(key)) ?: defaultValue
        }

        override suspend fun delete(key: String) {
            dataStore.edit { it.remove(booleanPreferencesKey(key)) }
        }
        override suspend fun write(key: String, value:  Boolean) {
            dataStore.edit { it[booleanPreferencesKey(key)] = value }
        }
    }

    class ObjectPrimitive<T>(
        preferences: DataStore<Preferences>,
        key: String,
        defaultValue: T,
        val serializer: (T) -> String,
        val deserializer: (String) -> T,
    ) : DataStorePreference<T>(preferences, key, defaultValue) {
        override fun changesFlow(
            datastore: DataStore<Preferences>,
            key: String,
            defaultValue: T
        ): Flow<T> {
            return dataStore.data.map { prefs ->
                prefs[stringPreferencesKey(key)]?.let { deserializer(it) } ?: defaultValue
            }
        }
        override suspend fun read(
            datastore: DataStore<Preferences>,
            key: String,
            defaultValue: T,
        ):  T {
            val string = dataStore.data.firstOrNull()?.get(stringPreferencesKey(key))
            return string?.let { deserializer(it) } ?: defaultValue
        }

        override suspend fun delete(key: String) {
            dataStore.edit { it.remove(stringPreferencesKey(key)) }
        }
        override suspend fun write(key: String, value:  T) {
            dataStore.edit { it[stringPreferencesKey(key)] = serializer(value) }
        }
    }
}

/**
 * Modified from https://github.com/aniyomiorg/aniyomi AndroidPreference
 * to use datastore instead of SharedPreferences
 */
class DatastorePreferenceStore(
    private val datastore: DataStore<Preferences>
) : PreferenceStore {

    override fun getString(key: String, defaultValue: String): Preference<String> {
        return DataStorePreference.StringPrimitive(datastore, key, defaultValue)
    }

    override fun getLong(key: String, defaultValue: Long): Preference<Long> {
        return DataStorePreference.LongPrimitive(datastore, key, defaultValue)
    }

    override fun getInt(key: String, defaultValue: Int): Preference<Int> {
        return DataStorePreference.IntPrimitive(datastore, key, defaultValue)
    }

    override fun getFloat(key: String, defaultValue: Float): Preference<Float> {
        return DataStorePreference.FloatPrimitive(datastore, key, defaultValue)
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Preference<Boolean> {
        return DataStorePreference.BooleanPrimitive(datastore, key, defaultValue)
    }

    override fun getStringSet(key: String, defaultValue: Set<String>): Preference<Set<String>> {
        return DataStorePreference.StringSetPrimitive(datastore, key, defaultValue)
    }

    override fun <T> getObject(
        key: String,
        defaultValue: T,
        serializer: (T) -> String,
        deserializer: (String) -> T
    ): Preference<T> {
        return DataStorePreference.ObjectPrimitive(
            datastore,
            key,
            defaultValue,
            serializer,
            deserializer
        )
    }

    override suspend fun getAll(): Map<Preferences.Key<*>, Any> {
        return datastore.data.firstOrNull()?.asMap() ?: emptyMap()
    }
}