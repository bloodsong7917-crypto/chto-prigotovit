package app.mealmate

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

class GeminiException(message: String) : Exception(message)

val AppJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    explicitNulls = false
    encodeDefaults = true
}

object Gemini {
    private const val BASE = "https://generativelanguage.googleapis.com/v1beta/models/"

    suspend inline fun <reified T> ask(apiKey: String, model: String, prompt: String, image: ByteArray? = null): T {
        val text = generate(apiKey, model, prompt, image)
        return try {
            AppJson.decodeFromString<T>(extractJson(text))
        } catch (e: Exception) {
            throw GeminiException("ИИ вернул ответ в неожиданном формате. Попробуйте ещё раз.")
        }
    }

    suspend fun generate(apiKey: String, model: String, prompt: String, image: ByteArray?): String =
        withContext(Dispatchers.IO) {
            val name = model.trim().ifEmpty { DEFAULT_MODEL }
            val conn = URL("$BASE$name:generateContent").openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "POST"
                conn.connectTimeout = 20_000
                conn.readTimeout = 180_000
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.setRequestProperty("x-goog-api-key", apiKey.trim())
                conn.outputStream.use { it.write(requestBody(prompt, image).toByteArray()) }

                val code = conn.responseCode
                val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (code !in 200..299) throw GeminiException(errorMessage(code, body))
                parseResponse(body)
            } catch (e: IOException) {
                throw GeminiException("Нет связи с сервером Gemini. Проверьте интернет (в некоторых регионах нужен VPN).")
            } finally {
                conn.disconnect()
            }
        }

    fun requestBody(prompt: String, image: ByteArray?): String = buildJsonObject {
        putJsonArray("contents") {
            addJsonObject {
                put("role", "user")
                putJsonArray("parts") {
                    if (image != null) addJsonObject {
                        putJsonObject("inline_data") {
                            put("mime_type", "image/jpeg")
                            put("data", Base64.getEncoder().encodeToString(image))
                        }
                    }
                    addJsonObject { put("text", prompt) }
                }
            }
        }
        putJsonObject("generationConfig") {
            put("responseMimeType", "application/json")
            put("temperature", 0.7)
        }
    }.toString()

    fun parseResponse(body: String): String {
        val root = try {
            AppJson.parseToJsonElement(body).jsonObject
        } catch (e: Exception) {
            throw GeminiException("Не удалось разобрать ответ сервера.")
        }
        val candidate = (root["candidates"] as? kotlinx.serialization.json.JsonArray)?.firstOrNull()?.jsonObject
        val parts = (candidate?.get("content") as? JsonObject)?.get("parts") as? kotlinx.serialization.json.JsonArray
        val text = parts.orEmpty()
            .map { it.jsonObject }
            .filter { it["thought"]?.jsonPrimitive?.contentOrNull != "true" }
            .mapNotNull { it["text"]?.jsonPrimitive?.contentOrNull }
            .joinToString("")
        if (text.isBlank()) {
            val block = (root["promptFeedback"] as? JsonObject)?.get("blockReason")?.jsonPrimitive?.contentOrNull
            val finish = candidate?.get("finishReason")?.jsonPrimitive?.contentOrNull
            throw GeminiException(
                when {
                    block != null -> "Запрос отклонён фильтром безопасности ($block)."
                    finish == "MAX_TOKENS" -> "Ответ получился слишком длинным. Попробуйте уменьшить запрос."
                    else -> "ИИ вернул пустой ответ. Попробуйте ещё раз."
                }
            )
        }
        return text
    }

    /** Достаёт JSON-объект из текста, даже если модель обернула его в ```json. */
    fun extractJson(text: String): String {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        return if (start >= 0 && end > start) text.substring(start, end + 1) else text
    }

    fun errorMessage(code: Int, body: String): String {
        val message = runCatching {
            AppJson.parseToJsonElement(body).jsonObject["error"]!!.jsonObject["message"]!!.jsonPrimitive.content
        }.getOrDefault("")
        return when {
            "location is not supported" in message ->
                "Gemini API недоступен в вашем регионе. Включите VPN и повторите."
            "API key" in message || code == 401 || code == 403 ->
                "Ключ Gemini API не подошёл. Проверьте его в настройках."
            code == 404 -> "Модель не найдена. Проверьте название модели в настройках."
            code == 429 -> "Превышен лимит запросов Gemini. Подождите минуту и повторите."
            code >= 500 -> "Сервер Gemini временно недоступен ($code). Повторите позже."
            else -> "Ошибка Gemini ($code): ${message.ifEmpty { "неизвестная ошибка" }}"
        }
    }
}

object Prompts {
    private const val RECIPE_FIELDS =
        "\"name\":\"название\",\"description\":\"одно предложение\",\"timeMinutes\":30,\"servings\":2," +
            "\"kcal\":450,\"protein\":25,\"fat\":15,\"carbs\":50," +
            "\"ingredients\":[{\"name\":\"продукт\",\"amount\":\"200 г\",\"have\":true}],\"steps\":[\"шаг\"]"

    private const val RECIPE_RULES =
        "kcal, protein, fat, carbs — числа (ккал и граммы) на ОДНУ порцию, посчитай их реалистично по составу. " +
            "amount — количество продукта сразу на все порции. Весь текст на русском языке. " +
            "Ответь только JSON без пояснений."

    private const val BASICS = "Соль, перец, сахар, растительное масло, вода и базовые специи считаются доступными всегда."

    fun recognize() =
        "Определи все продукты питания и ингредиенты на фото. Названия — на русском, в именительном падеже, " +
            "строчными буквами, без брендов и без повторов. Если еды на фото нет, верни пустой список. " +
            "Ответь только JSON вида {\"products\":[\"помидоры\",\"яйца\"]}."

    fun recipes(products: List<String>, servings: Int, fromPhoto: Boolean): String {
        val source = buildString {
            if (fromPhoto) append("Определи продукты на фото и перечисли их в поле products. ")
            if (products.isNotEmpty()) append("Продукты в наличии: ${products.joinToString(", ")}. ")
        }
        return "Ты кулинарный помощник. $source$BASICS " +
            "Предложи 5 разных блюд, которые можно приготовить в основном из имеющихся продуктов; " +
            "чем меньше нужно докупать, тем лучше. Каждое блюдо рассчитай на $servings порц. " +
            "Для продуктов, которых нет в наличии, ставь have=false. $RECIPE_RULES " +
            "Формат: {\"products\":[\"продукт\"],\"recipes\":[{$RECIPE_FIELDS}]}"
    }

    fun plan(days: Int, people: Int, kcalPerPerson: Int?, products: List<String>, wishes: String): String {
        val period = if (days == 1) "на 1 день" else "на $days дней"
        return buildString {
            append("Ты диетолог и кулинар. Составь сбалансированный рацион $period для $people чел. ")
            append("На каждый день: Завтрак, Обед, Ужин и Перекус (поле mealType). Блюда не должны повторяться. ")
            if (kcalPerPerson != null) append("Суточная калорийность на одного человека — около $kcalPerPerson ккал. ")
            if (products.isNotEmpty()) append("Постарайся использовать продукты, которые уже есть: ${products.joinToString(", ")}; для остальных ставь have=false. ")
            if (wishes.isNotBlank()) append("Пожелания: ${wishes.trim()}. ")
            append("Каждое блюдо рассчитай на $people порц. (servings=$people), шаги приготовления — кратко, не более 5. ")
            append("В shoppingList собери общий список покупок на весь период — только то, чего нет в наличии. ")
            append(RECIPE_RULES)
            append(" Формат: {\"days\":[{\"title\":\"День 1\",\"meals\":[{\"mealType\":\"Завтрак\",$RECIPE_FIELDS}]}],")
            append("\"shoppingList\":[{\"name\":\"продукт\",\"amount\":\"500 г\"}]}")
        }
    }

    fun estimate(text: String, fromPhoto: Boolean): String {
        val source = when {
            fromPhoto && text.isNotBlank() -> "Блюдо на фото, уточнение: «${text.trim()}»."
            fromPhoto -> "Блюдо и размер порции определи по фото."
            else -> "Съедено: «${text.trim()}»."
        }
        return "Оцени калорийность и БЖУ съеденного. $source Если размер порции не указан, возьми обычную порцию. " +
            "kcal — ккал, protein, fat, carbs — граммы, числа за всю порцию. name — короткое название на русском. " +
            "Ответь только JSON вида {\"name\":\"борщ, 300 г\",\"kcal\":180,\"protein\":8,\"fat\":7,\"carbs\":20}."
    }
}
