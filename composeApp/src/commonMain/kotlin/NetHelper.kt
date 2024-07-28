import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

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
    }

}

suspend inline fun <reified T> GET(url: String, crossinline block: HttpRequestBuilder.() -> Unit = {}): T =
    withContext(Dispatchers.IO) { Json.decodeFromString(NetHelper.client.get(url, block).bodyAsText()) }

