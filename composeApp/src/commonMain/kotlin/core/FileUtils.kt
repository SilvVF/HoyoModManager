package core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

suspend fun renameFolder(file: File, name: String): Result<File>  = withContext(Dispatchers.IO) {
    runCatching {
        val rename = Paths.get(file.parentFile.path, name).also {
            it.toFile().mkdirs()
        }

        Files.move(file.toPath(), rename, StandardCopyOption.REPLACE_EXISTING)
            .toFile()
            .also { file.deleteRecursively() }
    }
}