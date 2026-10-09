package app.mealmate.ui

import android.content.ActivityNotFoundException
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import app.mealmate.DishPhoto
import app.mealmate.Photos
import app.mealmate.R
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
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge)
}

@Composable
fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun AppCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        content = content,
    )
}

/** Четыре цветные плашки: калории, белки, жиры, углеводы. */
@Composable
fun MacroChips(kcal: Double, protein: Double, fat: Double, carbs: Double, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        MacroChip(kcal.r(), "ккал", Accent.Kcal, Modifier.weight(1.25f))
        MacroChip(protein.r(), "белки", Accent.Protein, Modifier.weight(1f))
        MacroChip(fat.r(), "жиры", Accent.Fat, Modifier.weight(1f))
        MacroChip(carbs.r(), "углев.", Accent.Carbs, Modifier.weight(1f))
    }
}

@Composable
private fun MacroChip(value: String, label: String, color: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
fun Stepper(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
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

/** Кнопки камеры и галереи; выбранное фото возвращается через [onPhoto]. */
@Composable
fun PhotoButtons(modifier: Modifier = Modifier, labels: Boolean = true, onPhoto: (Uri) -> Unit) {
    val context = LocalContext.current
    val shotUri = {
        val file = File(context.cacheDir, "photos").apply { mkdirs() }.resolve("shot.jpg")
        FileProvider.getUriForFile(context, context.packageName + ".files", file)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved) onPhoto(shotUri())
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onPhoto(uri)
    }
    val openCamera = {
        try {
            camera.launch(shotUri())
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Не найдено приложение камеры", Toast.LENGTH_SHORT).show()
        }
    }
    val openGallery = {
        gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (labels) {
            FilledTonalButton(onClick = openCamera, modifier = Modifier.weight(1f)) {
                Icon(painterResource(R.drawable.ic_camera), null, Modifier.size(18.dp))
                Text("Камера", Modifier.padding(start = 8.dp))
            }
            FilledTonalButton(onClick = openGallery, modifier = Modifier.weight(1f)) {
                Icon(painterResource(R.drawable.ic_image), null, Modifier.size(18.dp))
                Text("Галерея", Modifier.padding(start = 8.dp))
            }
        } else {
            FilledTonalIconButton(onClick = openCamera) {
                Icon(painterResource(R.drawable.ic_camera), "Сфотографировать")
            }
            FilledTonalIconButton(onClick = openGallery) {
                Icon(painterResource(R.drawable.ic_image), "Выбрать из галереи")
            }
        }
    }
}

@Composable
private fun mealColor(mealType: String): Color {
    val type = mealType.lowercase()
    return when {
        type.startsWith("завтрак") -> Accent.Fat
        type.startsWith("обед") -> Accent.Green
        type.startsWith("ужин") -> Accent.Dinner
        type.startsWith("перекус") -> Accent.Kcal
        else -> MaterialTheme.colorScheme.primary
    }
}

@Composable
fun RecipeCard(recipe: Recipe, onAddToDiary: (Recipe) -> Unit) {
    var expanded by rememberSaveable(recipe.name) { mutableStateOf(false) }
    val accent = mealColor(recipe.mealType)
    val context = LocalContext.current
    val query = recipe.photoQuery.ifBlank { recipe.name }
    var photo by remember(query) { mutableStateOf<DishPhoto?>(null) }
    var image by remember(query) { mutableStateOf<ImageBitmap?>(null) }
    var noPhoto by remember(query) { mutableStateOf(false) }
    LaunchedEffect(query) {
        photo = Photos.find(context, query)
        image = photo?.let { Photos.load(context, it.url) }
        noPhoto = image == null
    }
    AppCard(Modifier.clickable { expanded = !expanded }) {
        if (!noPhoto) {
            Box(
                Modifier.fillMaxWidth().height(170.dp).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                image?.let {
                    Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } ?: Icon(
                    painterResource(R.drawable.ic_restaurant), null,
                    Modifier.size(36.dp), tint = MaterialTheme.colorScheme.outline,
                )
            }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (recipe.mealType.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(accent))
                    Text(recipe.mealType, style = MaterialTheme.typography.labelMedium)
                }
            }
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(recipe.name, style = MaterialTheme.typography.titleLarge)
                    val missing = recipe.ingredients.count { !it.have }
                    Hint(
                        listOfNotNull(
                            if (recipe.timeMinutes > 0) "${recipe.timeMinutes} мин" else null,
                            "${recipe.servings} порц.",
                            if (missing > 0) "докупить $missing" else null,
                        ).joinToString(" · ")
                    )
                }
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Свернуть" else "Открыть рецепт",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            MacroChips(recipe.kcal, recipe.protein, recipe.fat, recipe.carbs)
            AnimatedVisibility(expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (recipe.description.isNotBlank()) Text(recipe.description, style = MaterialTheme.typography.bodyMedium)
                    if (recipe.ingredients.isNotEmpty()) {
                        Text("Ингредиенты", style = MaterialTheme.typography.titleMedium)
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
                        Text("Приготовление", style = MaterialTheme.typography.titleMedium)
                        recipe.steps.forEachIndexed { i, step ->
                            Text("${i + 1}. $step", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    TextButton(onClick = { onAddToDiary(recipe) }) { Text("В дневник") }
                    if (image != null) Hint("Фото для примера: ${photo?.credit}")
                }
            }
        }
    }
}
