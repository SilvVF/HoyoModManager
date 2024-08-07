package core.db

import androidx.room.Dao

@Dao
interface DatabaseDao: CharacterDao, ModDao, PlaylistDao, TagDao