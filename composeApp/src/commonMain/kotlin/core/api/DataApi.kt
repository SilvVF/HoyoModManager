package core.api

import core.model.Character
import core.model.Game

interface DataApi {

    val game: Game

    val elements: List<String>

    fun avatarIconUrl(name: String): String

    suspend fun elementList(): List<String>

    suspend fun characterList(): List<String>

    suspend fun characterData(path: String): Character
}