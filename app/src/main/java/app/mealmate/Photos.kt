package app.mealmate

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.int
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Фото блюда: адрес картинки и подпись «автор, лицензия». Пустой [url] — фото не нашлось. */
@Serializable
data class DishPhoto(val url: String = "", val credit: String = "")

/** Подбирает к блюду похожее фото из Wikimedia Commons и кэширует его на телефоне. */
object Photos {
    // Wikimedia отклоняет запросы без осмысленного User-Agent
    private const val USER_AGENT = "ChtoPrigotovit/1.2 (Android app)"
    private const val API = "https://commons.wikimedia.org/w/api.php?action=query&format=json" +
        "&generator=search&gsrnamespace=6&gsrlimit=1&prop=imageinfo&iiprop=url%7Cextmetadata" +
        "&iiextmetadatafilter=Artist%7CLicenseShortName&iiurlwidth=960&gsrsearch="

    private val network = Semaphore(3)
    private val bitmaps = LruCache<String, ImageBitmap>(12)

    private fun open(url: String) = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 15_000
        readTimeout = 30_000
        setRequestProperty("User-Agent", USER_AGENT)
    }

    /** Ищет фото по запросу; результат (в том числе «не найдено») запоминается. */
    suspend fun find(context: Context, query: String): DishPhoto? = withContext(Dispatchers.IO) {
        val key = query.trim().lowercase()
        if (key.isEmpty()) return@withContext null
        val prefs = context.getSharedPreferences("photos", 0)
        prefs.getString(key, null)?.let { saved ->
            runCatching { AppJson.decodeFromString<DishPhoto>(saved) }.getOrNull()?.let { return@withContext it }
        }
        val photo = try {
            network.withPermit {
                val conn = open(API + URLEncoder.encode("$key filetype:bitmap", "UTF-8"))
                try {
                    parseSearch(conn.inputStream.bufferedReader().use { it.readText() })
                } finally {
                    conn.disconnect()
                }
            }
        } catch (e: Exception) {
            return@withContext null // нет сети — попробуем в следующий раз
        }
        prefs.edit().putString(key, AppJson.encodeToString(photo)).apply()
        photo
    }

    fun parseSearch(body: String): DishPhoto {
        val pages = AppJson.parseToJsonElement(body).jsonObject["query"]?.jsonObject?.get("pages")?.jsonObject
            ?: return DishPhoto()
        val info = pages.values.map { it.jsonObject }
            .minByOrNull { it["index"]?.jsonPrimitive?.int ?: 0 }
            ?.get("imageinfo")?.jsonArray?.firstOrNull()?.jsonObject
            ?: return DishPhoto()
        val meta = info["extmetadata"]?.jsonObject
        fun field(name: String) = meta?.get(name)?.jsonObject?.get("value")?.jsonPrimitive?.content.orEmpty()
        val author = field("Artist").replace(Regex("<[^>]*>"), "").replace(Regex("\\s+"), " ").trim().take(60)
        val credit = listOf(author, field("LicenseShortName"), "Wikimedia Commons").filter { it.isNotBlank() }
        return DishPhoto(info["thumburl"]?.jsonPrimitive?.content.orEmpty(), credit.joinToString(", "))
    }

    /** Загружает картинку: из памяти, с диска или из сети. */
    suspend fun load(context: Context, url: String): ImageBitmap? = withContext(Dispatchers.IO) {
        if (url.isEmpty()) return@withContext null
        bitmaps.get(url)?.let { return@withContext it }
        try {
            val file = if (url.startsWith("/")) File(url) else {
                val cached = File(context.cacheDir, "dishes").apply { mkdirs() }.resolve("${url.hashCode()}.jpg")
                if (!cached.exists()) network.withPermit {
                    val conn = open(url)
                    try {
                        val part = File(cached.path + ".part")
                        conn.inputStream.use { input -> part.outputStream().use { input.copyTo(it) } }
                        part.renameTo(cached)
                    } finally {
                        conn.disconnect()
                    }
                }
                cached
            }
            BitmapFactory.decodeFile(file.path)?.asImageBitmap()?.also { bitmaps.put(url, it) }
        } catch (e: Exception) {
            null
        }
    }
}
