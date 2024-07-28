package net

import com.seiko.imageloader.cache.disk.DiskCache
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.cache.storage.CacheStorage
import io.ktor.client.plugins.cache.storage.FileStorage
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

object NetHelper {

    val json = Json {
        isLenient = true
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json = json)
        }
        install(HttpCache) {
            val cacheFile = Files.createDirectories(File(OS.getCacheDir(), "network").toPath()).toFile()
            publicStorage(FileStorage(cacheFile))
        }
    }

}

suspend inline fun <reified T> GET(url: String, crossinline block: HttpRequestBuilder.() -> Unit = {}): T =
    withContext(Dispatchers.IO) { Json.decodeFromString(NetHelper.client.get(url, block).bodyAsText()) }

