package core.api

import core.model.Character
import core.model.Game

interface DataApi {

    val skinCategoryId: Int

    val game: Game

    val elements: List<String>

    suspend fun elementList(): List<String>

    suspend fun characterList(): List<Character>
}