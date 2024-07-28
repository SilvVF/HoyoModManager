package core.model

data class Character(
    val id: Int,
    val game: Game,
    val name: String,
    val avatarUrl: String,
    val element: String
)
