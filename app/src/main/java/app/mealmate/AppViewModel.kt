package app.mealmate

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import java.time.LocalDate

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("data", 0)
    private var job: Job? = null

    var data by mutableStateOf(load())
        private set

    /** Текст текущей операции с ИИ, пока она выполняется. */
    var busy by mutableStateOf<String?>(null)
        private set
    var error by mutableStateOf<String?>(null)

    /** Результат оценки блюда, который экран дневника подставляет в форму. */
    var dishDraft by mutableStateOf<DishEstimate?>(null)

    private fun load(): AppData {
        val saved = prefs.getString("d", null)
            ?.let { runCatching { AppJson.decodeFromString<AppData>(it) }.getOrNull() }
            ?: AppData()
        val today = LocalDate.now().toString()
        return if (saved.diaryDate == today) saved else saved.copy(diaryDate = today, diary = emptyList())
    }

    fun update(change: (AppData) -> AppData) {
        data = change(data)
        prefs.edit().putString("d", AppJson.encodeToString(data)).apply()
    }

    /** Обнуляет дневник, если наступил новый день. */
    fun rollDiary() {
        val today = LocalDate.now().toString()
        if (data.diaryDate != today) update { it.copy(diaryDate = today, diary = emptyList()) }
    }

    fun addProducts(text: String) {
        val items = text.split(',', ';', '\n').map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        if (items.isNotEmpty()) update { it.copy(products = (it.products + items).distinct()) }
    }

    fun removeProduct(name: String) = update { it.copy(products = it.products - name) }

    fun addToDiary(name: String, kcal: Double, protein: Double, fat: Double, carbs: Double) {
        rollDiary()
        update { it.copy(diary = it.diary + DiaryEntry(System.currentTimeMillis(), name, kcal, protein, fat, carbs)) }
    }

    fun removeFromDiary(id: Long) = update { d -> d.copy(diary = d.diary.filterNot { it.id == id }) }

    fun cancel() {
        job?.cancel()
    }

    private fun ai(message: String, block: suspend () -> Unit) {
        if (busy != null) return
        if (data.apiKey.isBlank()) {
            error = "Сначала укажите ключ Gemini API в настройках (значок шестерёнки вверху)."
            return
        }
        busy = message
        job = viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.message ?: "Неизвестная ошибка."
            } finally {
                busy = null
            }
        }
    }

    private suspend inline fun <reified T> ask(prompt: String, photo: Uri? = null): T {
        val image = photo?.let { loadJpeg(getApplication(), it) }
        return Gemini.ask(data.apiKey, data.model, prompt, image)
    }

    fun recognizeProducts(photo: Uri) = ai("Распознаю продукты на фото…") {
        val result: PhotoResult = ask(Prompts.recognize(), photo)
        if (result.products.isEmpty()) error = "На фото не удалось найти продукты."
        else addProducts(result.products.joinToString(","))
    }

    fun findRecipes(photo: Uri? = null) {
        if (photo == null && data.products.isEmpty()) {
            error = "Список продуктов пуст. Добавьте продукты или подберите рецепты по фото."
            return
        }
        ai(if (photo != null) "Смотрю фото и подбираю рецепты…" else "Подбираю рецепты…") {
            // по фото подбираем только из того, что на снимке, список продуктов лишь пополняем
            val known = if (photo != null) emptyList() else data.products
            val result: RecipesResult = ask(Prompts.recipes(known, data.servings, photo != null), photo)
            if (photo != null) addProducts(result.products.joinToString(","))
            if (result.recipes.isEmpty()) error = "Не получилось подобрать рецепты. Попробуйте ещё раз."
            else update { it.copy(recipes = result.recipes) }
        }
    }

    fun makePlan(days: Int, useProducts: Boolean, useTarget: Boolean, wishes: String) =
        ai(if (days == 1) "Составляю рацион на день…" else "Составляю рацион на неделю, это может занять пару минут…") {
            val people = data.people
            val kcal = if (useTarget) Nutrition.target(data.profile).kcal.toInt() else null
            val products = if (useProducts) data.products else emptyList()
            val plan: MealPlan = ask(Prompts.plan(days, people, kcal, products, wishes))
            if (plan.days.isEmpty()) error = "Не получилось составить рацион. Попробуйте ещё раз."
            else update { it.copy(plan = plan.copy(people = people)) }
        }

    fun estimateDish(text: String, photo: Uri? = null) {
        if (photo == null && text.isBlank()) {
            error = "Опишите, что вы съели, например: «тарелка борща и два куска хлеба»."
            return
        }
        ai("Оцениваю калорийность…") {
            dishDraft = ask<DishEstimate>(Prompts.estimate(text, photo != null), photo)
        }
    }
}
