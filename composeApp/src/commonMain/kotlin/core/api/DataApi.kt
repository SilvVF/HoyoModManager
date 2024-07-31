package core.api

import core.model.Character
import core.model.Game
import java.io.File

interface DataApi {

    val game: Game

    val elements: List<String>

    suspend fun elementList(): List<String>

    suspend fun characterList(): List<Character>
}