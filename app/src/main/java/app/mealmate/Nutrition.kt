package app.mealmate

import kotlin.math.roundToInt

data class Macro(val kcal: Double, val protein: Double, val fat: Double, val carbs: Double)

object Nutrition {
    val activities = listOf(
        "Минимальная (сидячая работа)" to 1.2,
        "Лёгкая (тренировки 1–3 раза в неделю)" to 1.375,
        "Средняя (тренировки 3–5 раз в неделю)" to 1.55,
        "Высокая (тренировки 6–7 раз в неделю)" to 1.725,
        "Очень высокая (физический труд, спорт)" to 1.9,
    )

    // название, множитель калорий, доли Б/Ж/У в калорийности
    private data class Goal(val title: String, val factor: Double, val p: Double, val f: Double, val c: Double)

    private val goalList = listOf(
        Goal("Похудение", 0.85, 0.30, 0.30, 0.40),
        Goal("Поддержание", 1.0, 0.25, 0.30, 0.45),
        Goal("Набор массы", 1.15, 0.25, 0.25, 0.50),
    )
    val goals = goalList.map { it.title }

    /** Основной обмен по формуле Миффлина — Сан-Жеора. */
    fun bmr(p: Profile): Double =
        10 * p.weightKg + 6.25 * p.heightCm - 5 * p.age + if (p.male) 5 else -161

    /** Расход с учётом активности. */
    fun tdee(p: Profile): Double =
        bmr(p) * activities[p.activity.coerceIn(activities.indices)].second

    /** Суточная норма с учётом цели. */
    fun target(p: Profile): Macro {
        val g = goalList[p.goal.coerceIn(goalList.indices)]
        val kcal = (tdee(p) * g.factor).coerceAtLeast(1200.0)
        return Macro(kcal, kcal * g.p / 4, kcal * g.f / 9, kcal * g.c / 4)
    }
}

fun Double.r(): String = roundToInt().toString()
