package core.model

import core.db.DB

enum class Game(val data: Byte) {
    Genshin(0x01), 
    StarRail(0x02), 
    ZZZ(0x03);
    
    companion object {
        fun fromByte(byte: Byte) = entries.first { it.data == byte }
    }
}