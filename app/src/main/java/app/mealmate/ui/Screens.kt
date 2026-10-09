@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package app.mealmate.ui

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.mealmate.AppViewModel
import app.mealmate.Nutrition
import app.mealmate.Recipe
import app.mealmate.r

@Composable
private fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { i, title ->
            SegmentedButton(
                selected = i == selected,
                onClick = { onSelect(i) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
                icon = {},
            ) { Text(title, maxLines = 1) }
        }
    }
}

@Composable
private fun SwitchRow(text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(text, modifier = Modifier.weight(1f).padding(end = 8.dp), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun rememberDiaryAdder(vm: AppViewModel): (Recipe) -> Unit {
    val context = LocalContext.current
    return { recipe ->
        vm.addToDiary(recipe.name, recipe.kcal, recipe.protein, recipe.fat, recipe.carbs)
        Toast.makeText(context, "Добавлено в дневник", Toast.LENGTH_SHORT).show()
    }
}

// ---------- Продукты ----------

private val chipColors = listOf(Accent.Green, Accent.Lime, Accent.Fat, Accent.Kcal, Accent.Carbs, Accent.Protein)

@Composable
private fun ProductChip(name: String, onRemove: () -> Unit) {
    val color = chipColors[Math.floorMod(name.hashCode(), chipColors.size)]
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.3f))
            .clickable(onClick = onRemove)
            .padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(name, style = MaterialTheme.typography.bodyMedium)
        Icon(Icons.Default.Close, contentDescription = "Удалить $name", modifier = Modifier.size(16.dp))
    }
}

@Composable
fun ProductsScreen(vm: AppViewModel, onShowRecipes: () -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }
    val add = {
        vm.addProducts(input)
        input = ""
    }
    Screen {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("Продукты через запятую") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { add() }),
                modifier = Modifier.weight(1f),
            )
            FilledIconButton(onClick = add, enabled = input.isNotBlank()) {
                Icon(Icons.Default.Add, contentDescription = "Добавить")
            }
        }
        PhotoButtons(Modifier.fillMaxWidth()) { vm.recognizeProducts(it) }

        val products = vm.data.products
        if (products.isEmpty()) {
            Hint("Пока пусто — добавьте продукты или сфотографируйте их.")
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                products.forEach { name -> ProductChip(name) { vm.removeProduct(name) } }
            }
            Button(
                onClick = {
                    vm.findRecipes()
                    onShowRecipes()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Подобрать рецепты · ${products.size}") }
            TextButton(onClick = { vm.update { it.copy(products = emptyList()) } }) { Text("Очистить") }
        }
    }
}

// ---------- Рецепты ----------

@Composable
fun RecipesScreen(vm: AppViewModel) {
    val addToDiary = rememberDiaryAdder(vm)
    Screen {
        Stepper("Порций", vm.data.servings, 1..12) { n -> vm.update { it.copy(servings = n) } }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.findRecipes() }, modifier = Modifier.weight(1f)) {
                Text("Из продуктов · ${vm.data.products.size}")
            }
            PhotoButtons(labels = false) { vm.findRecipes(it) }
        }
        vm.data.recipes.forEach { RecipeCard(it, addToDiary) }
    }
}

// ---------- Рацион ----------

@Composable
fun PlanScreen(vm: AppViewModel) {
    var period by rememberSaveable { mutableIntStateOf(0) }
    var useProducts by rememberSaveable { mutableStateOf(true) }
    var useTarget by rememberSaveable { mutableStateOf(true) }
    var wishes by rememberSaveable { mutableStateOf("") }
    val addToDiary = rememberDiaryAdder(vm)
    val target = Nutrition.target(vm.data.profile)

    Screen {
        Segmented(listOf("1 день", "Неделя"), period) { period = it }
        Stepper("Человек", vm.data.people, 1..12) { n -> vm.update { it.copy(people = n) } }
        SwitchRow("Мои продукты · ${vm.data.products.size}", useProducts) { useProducts = it }
        SwitchRow("Моя норма · ${target.kcal.r()} ккал", useTarget) { useTarget = it }
        OutlinedTextField(
            value = wishes,
            onValueChange = { wishes = it },
            label = { Text("Пожелания") },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { vm.makePlan(if (period == 0) 1 else 7, useProducts, useTarget, wishes) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Составить рацион") }

        val plan = vm.data.plan ?: return@Screen
        plan.days.forEach { day ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle(day.title)
                MacroChips(
                    day.meals.sumOf { it.kcal },
                    day.meals.sumOf { it.protein },
                    day.meals.sumOf { it.fat },
                    day.meals.sumOf { it.carbs },
                )
            }
            day.meals.forEach { RecipeCard(it, addToDiary) }
        }
        if (plan.shoppingList.isNotEmpty()) {
            AppCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SectionTitle("Купить")
                    plan.shoppingList.forEach {
                        Text(
                            "• ${it.name}" + if (it.amount.isNotBlank()) " — ${it.amount}" else "",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

// ---------- Калории ----------

@Composable
private fun NumberField(label: String, value: String, modifier: Modifier = Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { text -> onChange(text.filter { it.isDigit() || it == '.' || it == ',' }.take(6)) },
        label = { Text(label, maxLines = 1) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

private fun String.num(): Double = replace(',', '.').toDoubleOrNull() ?: 0.0

@Composable
private fun HeroStat(value: String, label: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = Color.White)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
    }
}

/** Кольцо «съедено / норма» с остатком калорий в центре. */
@Composable
private fun CalorieRing(eaten: Double, target: Double, modifier: Modifier = Modifier) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val over = eaten > target
    val color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val fraction = (eaten / target).toFloat().coerceIn(0f, 1f)
    Box(modifier.size(132.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
            val topLeft = Offset(stroke / 2, stroke / 2)
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            drawArc(color, -90f, 360f * fraction, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(kotlin.math.abs(target - eaten).r(), style = MaterialTheme.typography.headlineMedium, color = color)
            Hint(if (over) "перебор" else "осталось")
        }
    }
}

@Composable
private fun MacroBar(label: String, eaten: Double, target: Double, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
            Hint("${eaten.r()} / ${target.r()} г")
        }
        LinearProgressIndicator(
            progress = { (eaten / target).toFloat().coerceIn(0f, 1f) },
            color = color,
            trackColor = color.copy(alpha = 0.2f),
            drawStopIndicator = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun CaloriesScreen(vm: AppViewModel) {
    val profile = vm.data.profile
    var age by rememberSaveable { mutableStateOf(profile.age.toString()) }
    var height by rememberSaveable { mutableStateOf(profile.heightCm.toString()) }
    var weight by rememberSaveable { mutableStateOf(profile.weightKg.toString().removeSuffix(".0")) }
    var activityOpen by rememberSaveable { mutableStateOf(false) }
    val target = Nutrition.target(profile)

    LaunchedEffect(Unit) { vm.rollDiary() }

    Screen {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(headerBrush())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(target.kcal.r(), style = MaterialTheme.typography.displayMedium, color = Color.White)
                Text(
                    "ккал в сутки",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Row(Modifier.fillMaxWidth()) {
                HeroStat("${target.protein.r()} г", "белки", Modifier.weight(1f))
                HeroStat("${target.fat.r()} г", "жиры", Modifier.weight(1f))
                HeroStat("${target.carbs.r()} г", "углеводы", Modifier.weight(1f))
            }
        }

        Segmented(listOf("Мужчина", "Женщина"), if (profile.male) 0 else 1) { i ->
            vm.update { it.copy(profile = it.profile.copy(male = i == 0)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Возраст", age, Modifier.weight(1f)) { text ->
                age = text
                vm.update { it.copy(profile = it.profile.copy(age = text.num().toInt().coerceIn(10, 110))) }
            }
            NumberField("Рост, см", height, Modifier.weight(1f)) { text ->
                height = text
                vm.update { it.copy(profile = it.profile.copy(heightCm = text.num().toInt().coerceIn(100, 250))) }
            }
            NumberField("Вес, кг", weight, Modifier.weight(1f)) { text ->
                weight = text
                vm.update { it.copy(profile = it.profile.copy(weightKg = text.num().coerceIn(30.0, 300.0))) }
            }
        }
        Box {
            OutlinedButton(onClick = { activityOpen = true }, modifier = Modifier.fillMaxWidth()) {
                Text(Nutrition.activities[profile.activity.coerceIn(Nutrition.activities.indices)].first)
            }
            DropdownMenu(expanded = activityOpen, onDismissRequest = { activityOpen = false }) {
                Nutrition.activities.forEachIndexed { i, (title, _) ->
                    DropdownMenuItem(text = { Text(title) }, onClick = {
                        vm.update { it.copy(profile = it.profile.copy(activity = i)) }
                        activityOpen = false
                    })
                }
            }
        }
        Segmented(Nutrition.goals, profile.goal) { i -> vm.update { it.copy(profile = it.profile.copy(goal = i)) } }

        // ----- дневник -----
        val diary = vm.data.diary
        SectionTitle("Сегодня")
        AppCard {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CalorieRing(diary.sumOf { it.kcal }, target.kcal)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MacroBar("Белки", diary.sumOf { it.protein }, target.protein, Accent.Protein)
                    MacroBar("Жиры", diary.sumOf { it.fat }, target.fat, Accent.Fat)
                    MacroBar("Углеводы", diary.sumOf { it.carbs }, target.carbs, Accent.Carbs)
                }
            }
        }
        diary.forEach { entry ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(entry.name, style = MaterialTheme.typography.bodyLarge)
                    Hint("${entry.kcal.r()} ккал · Б ${entry.protein.r()} · Ж ${entry.fat.r()} · У ${entry.carbs.r()}")
                }
                IconButton(onClick = { vm.removeFromDiary(entry.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить ${entry.name}")
                }
            }
        }

        DiaryForm(vm)
    }
}

@Composable
private fun DiaryForm(vm: AppViewModel) {
    var name by rememberSaveable { mutableStateOf("") }
    var kcal by rememberSaveable { mutableStateOf("") }
    var protein by rememberSaveable { mutableStateOf("") }
    var fat by rememberSaveable { mutableStateOf("") }
    var carbs by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(vm.dishDraft) {
        val draft = vm.dishDraft ?: return@LaunchedEffect
        if (draft.name.isNotBlank()) name = draft.name
        kcal = draft.kcal.r()
        protein = draft.protein.r()
        fat = draft.fat.r()
        carbs = draft.carbs.r()
        vm.dishDraft = null
    }

    OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Что съели") },
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    )
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilledTonalButton(onClick = { vm.estimateDish(name) }, modifier = Modifier.weight(1f)) {
            Text("Оценить калории")
        }
        PhotoButtons(labels = false) { vm.estimateDish(name, it) }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        NumberField("ккал", kcal, Modifier.weight(1.3f)) { kcal = it }
        NumberField("Б", protein, Modifier.weight(1f)) { protein = it }
        NumberField("Ж", fat, Modifier.weight(1f)) { fat = it }
        NumberField("У", carbs, Modifier.weight(1f)) { carbs = it }
    }
    Button(
        onClick = {
            vm.addToDiary(name.trim().ifEmpty { "Приём пищи" }, kcal.num(), protein.num(), fat.num(), carbs.num())
            name = ""; kcal = ""; protein = ""; fat = ""; carbs = ""
        },
        enabled = kcal.num() > 0,
        modifier = Modifier.fillMaxWidth(),
    ) { Text("В дневник") }
}
