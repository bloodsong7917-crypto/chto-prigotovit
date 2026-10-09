package app.mealmate.ui

import android.content.ActivityNotFoundException
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import app.mealmate.Recipe
import app.mealmate.r
import java.io.File

@Composable
fun Screen(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

fun macroText(kcal: Double, protein: Double, fat: Double, carbs: Double) =
    "${kcal.r()} ккал · Б ${protein.r()} · Ж ${fat.r()} · У ${carbs.r()} г"

@Composable
fun Stepper(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f))
        FilledTonalIconButton(onClick = { onChange(value - 1) }, enabled = value > range.first) { Text("−") }
        Text(
            value.toString(),
            modifier = Modifier.width(40.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
        )
        FilledTonalIconButton(onClick = { onChange(value + 1) }, enabled = value < range.last) { Text("+") }
    }
}

/** Кнопки «Камера» и «Галерея»; выбранное фото возвращается через [onPhoto]. */
@Composable
fun PhotoButtons(onPhoto: (Uri) -> Unit) {
    val context = LocalContext.current
    val shotUri = remember {
        val file = File(context.cacheDir, "photos").apply { mkdirs() }.resolve("shot.jpg")
        FileProvider.getUriForFile(context, context.packageName + ".files", file)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved) onPhoto(shotUri)
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onPhoto(uri)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = {
                try {
                    camera.launch(shotUri)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(context, "Не найдено приложение камеры", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.weight(1f),
        ) { Text("📷 Камера") }
        OutlinedButton(
            onClick = {
                gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            modifier = Modifier.weight(1f),
        ) { Text("🖼 Галерея") }
    }
}

@Composable
fun RecipeCard(recipe: Recipe, onAddToDiary: (Recipe) -> Unit) {
    var expanded by rememberSaveable(recipe.name) { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (recipe.mealType.isNotBlank()) {
                Text(
                    recipe.mealType.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(recipe.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                macroText(recipe.kcal, recipe.protein, recipe.fat, recipe.carbs) + " на порцию",
                style = MaterialTheme.typography.bodyMedium,
            )
            val missing = recipe.ingredients.count { !it.have }
            Hint(
                listOfNotNull(
                    if (recipe.timeMinutes > 0) "${recipe.timeMinutes} мин" else null,
                    "порций: ${recipe.servings}",
                    if (missing > 0) "докупить: $missing" else null,
                ).joinToString(" · ")
            )
            AnimatedVisibility(expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (recipe.description.isNotBlank()) Text(recipe.description, style = MaterialTheme.typography.bodyMedium)
                    if (recipe.ingredients.isNotEmpty()) {
                        Text("Ингредиенты", fontWeight = FontWeight.SemiBold)
                        recipe.ingredients.forEach {
                            Text(
                                "• ${it.name}" + (if (it.amount.isNotBlank()) " — ${it.amount}" else "") +
                                    if (it.have) "" else "  (докупить)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (it.have) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    if (recipe.steps.isNotEmpty()) {
                        Text("Приготовление", fontWeight = FontWeight.SemiBold)
                        recipe.steps.forEachIndexed { i, step ->
                            Text("${i + 1}. $step", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    TextButton(onClick = { onAddToDiary(recipe) }) { Text("Съел(а) порцию — в дневник") }
                }
            }
            if (!expanded) Hint("Нажмите, чтобы открыть рецепт")
        }
    }
}
