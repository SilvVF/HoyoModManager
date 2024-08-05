package core.db

import androidx.room.Dao
import core.model.CharacterDao
import core.model.ModDao
import core.model.PlaylistDao
import core.model.PrefsDao
import core.model.TagDao

@Dao
interface DatabaseDao: CharacterDao, ModDao, PlaylistDao, TagDao, PrefsDao