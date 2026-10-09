package app.mealmate

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import app.mealmate.ui.AppTheme
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.serialization.encodeToString
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/** Рисует экраны с примером данных в app/build/shots — чтобы посмотреть оформление без телефона. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h1150dp-xxhdpi")
class ScreenshotTest {
    @get:Rule
    val rule = createComposeRule()

    private val borscht = Recipe(
        name = "Борщ со сметаной", photoQuery = "borscht", description = "Классический наваристый борщ.", timeMinutes = 90, servings = 2,
        kcal = 320.0, protein = 18.0, fat = 12.0, carbs = 34.0,
        ingredients = listOf(Ingredient("свёкла", "2 шт"), Ingredient("говядина", "300 г", have = false)),
        steps = listOf("Сварить бульон.", "Добавить овощи и варить до готовности."),
    )
    private val sample = AppData(
        products = listOf("яйца", "молоко", "сыр", "помидоры", "куриное филе", "рис", "лук", "свёкла", "сметана"),
        recipes = listOf(borscht, borscht.copy(name = "Омлет с сыром и помидорами", photoQuery = "", timeMinutes = 15, kcal = 410.0, protein = 26.0, fat = 30.0, carbs = 8.0)),
        plan = MealPlan(
            days = listOf(
                PlanDay(
                    "День 1",
                    listOf(
                        borscht.copy(mealType = "Завтрак", photoQuery = "", name = "Овсянка с ягодами", timeMinutes = 10),
                        borscht.copy(mealType = "Обед"),
                        borscht.copy(mealType = "Ужин", photoQuery = "", name = "Курица с рисом", timeMinutes = 40),
                        borscht.copy(mealType = "Перекус", photoQuery = "", name = "Творог с мёдом", timeMinutes = 5),
                    ),
                )
            ),
            shoppingList = listOf(Ingredient("говядина", "300 г"), Ingredient("овсяные хлопья", "200 г")),
            people = 2,
        ),
        diaryDate = LocalDate.now().toString(),
        diary = listOf(
            DiaryEntry(1, "Овсянка с ягодами", 350.0, 12.0, 8.0, 58.0),
            DiaryEntry(2, "Борщ со сметаной", 640.0, 36.0, 24.0, 68.0),
        ),
    )

    private fun shot(tab: Int, name: String, expand: String? = null) {
        ApplicationProvider.getApplicationContext<Application>().getSharedPreferences("data", 0)
            .edit().putString("d", AppJson.encodeToString(sample)).commit()
        // фото берём из файла, чтобы тест не ходил в сеть; остальные блюда остаются без фото
        val app = ApplicationProvider.getApplicationContext<Application>()
        val photos = app.getSharedPreferences("photos", 0).edit()
        val file = java.io.File("src/test/resources/dish.jpg").absolutePath
        photos.putString("borscht", AppJson.encodeToString(DishPhoto(file, "Juerg Vollmer, CC BY-SA 2.0, Wikimedia Commons")))
        sample.recipes.drop(1).forEach { photos.putString(it.name.lowercase(), AppJson.encodeToString(DishPhoto())) }
        sample.plan!!.days.flatMap { it.meals }.forEach { if (it.photoQuery.isBlank()) photos.putString(it.name.lowercase(), AppJson.encodeToString(DishPhoto())) }
        photos.commit()
        rule.setContent { AppTheme { App(startTab = tab) } }
        if (expand != null) rule.onNodeWithText(expand).performClick()
        Thread.sleep(1500)
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/shots/$name.png")
    }

    @Test fun products() = shot(0, "1_products")
    @Test fun recipes() = shot(1, "2_recipes", expand = "Борщ со сметаной")
    @Test fun plan() = shot(2, "3_plan")
    @Test fun calories() = shot(3, "4_calories")

    @Test
    @Config(qualifiers = "+night")
    fun caloriesDark() = shot(3, "5_calories_dark")
}
