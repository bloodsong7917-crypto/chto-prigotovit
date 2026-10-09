package app.mealmate

import kotlinx.serialization.Serializable

@Serializable
data class Ingredient(
    val name: String = "",
    val amount: String = "",
    val have: Boolean = true,
)

/** КБЖУ указаны на одну порцию, количество ингредиентов — на все [servings] порций. */
@Serializable
data class Recipe(
    val mealType: String = "",
    val name: String = "",
    val description: String = "",
    val timeMinutes: Int = 0,
    val servings: Int = 1,
    val kcal: Double = 0.0,
    val protein: Double = 0.0,
    val fat: Double = 0.0,
    val carbs: Double = 0.0,
    val ingredients: List<Ingredient> = emptyList(),
    val steps: List<String> = emptyList(),
)

@Serializable
data class PhotoResult(val products: List<String> = emptyList())

@Serializable
data class RecipesResult(
    val products: List<String> = emptyList(),
    val recipes: List<Recipe> = emptyList(),
)

@Serializable
data class PlanDay(
    val title: String = "",
    val meals: List<Recipe> = emptyList(),
)

@Serializable
data class MealPlan(
    val days: List<PlanDay> = emptyList(),
    val shoppingList: List<Ingredient> = emptyList(),
    val people: Int = 1,
)

@Serializable
data class DishEstimate(
    val name: String = "",
    val kcal: Double = 0.0,
    val protein: Double = 0.0,
    val fat: Double = 0.0,
    val carbs: Double = 0.0,
)

@Serializable
data class Profile(
    val male: Boolean = true,
    val age: Int = 30,
    val heightCm: Int = 170,
    val weightKg: Double = 70.0,
    val activity: Int = 1,
    val goal: Int = 1,
)

@Serializable
data class DiaryEntry(
    val id: Long = 0,
    val name: String = "",
    val kcal: Double = 0.0,
    val protein: Double = 0.0,
    val fat: Double = 0.0,
    val carbs: Double = 0.0,
)

@Serializable
data class AppData(
    val apiKey: String = "",
    val model: String = DEFAULT_MODEL,
    val products: List<String> = emptyList(),
    val servings: Int = 2,
    val recipes: List<Recipe> = emptyList(),
    val people: Int = 2,
    val plan: MealPlan? = null,
    val profile: Profile = Profile(),
    val diaryDate: String = "",
    val diary: List<DiaryEntry> = emptyList(),
)

const val DEFAULT_MODEL = "gemini-flash-latest"
