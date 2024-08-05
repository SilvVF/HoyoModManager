package core.db

import androidx.room.TypeConverter
import core.model.Game
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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