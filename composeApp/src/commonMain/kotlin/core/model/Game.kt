package core.model

import core.db.DB

enum class Game {
    Genshin, StarRail, ZZZ;

    fun toByte(): Byte = when(this) {
        Genshin -> DB.GENSHIN
        StarRail -> DB.STAR_RAIL
        ZZZ -> DB.ZZZ
    }

    companion object {

        fun fromByte(byte: Byte) = when(byte){
            DB.GENSHIN -> Genshin
            DB.STAR_RAIL -> StarRail
            DB.ZZZ -> ZZZ
            else -> error("Invalid byte")
        }
    }
}