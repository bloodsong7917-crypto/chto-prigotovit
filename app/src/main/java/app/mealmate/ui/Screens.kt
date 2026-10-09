@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package app.mealmate.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
        Text(text, modifier = Modifier.weight(1f).padding(end = 8.dp))
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

@Composable
fun ProductsScreen(vm: AppViewModel, onShowRecipes: () -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }
    val add = {
        vm.addProducts(input)
        input = ""
    }
    Screen {
        SectionTitle("Что есть дома")
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("Продукт или несколько через запятую") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { add() }),
                modifier = Modifier.weight(1f),
            )
            Button(onClick = add, enabled = input.isNotBlank()) { Text("Добавить") }
        }
        Hint("Или сфотографируйте холодильник, полку или покупки — продукты добавятся в список сами.")
        PhotoButtons { vm.recognizeProducts(it) }

        val products = vm.data.products
        if (products.isEmpty()) {
            Hint("Список пока пуст.")
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                products.forEach { name ->
                    InputChip(
                        selected = false,
                        onClick = { vm.removeProduct(name) },
                        label = { Text(name) },
                        trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Удалить $name") },
                    )
                }
            }
            Button(
                onClick = {
                    vm.findRecipes()
                    onShowRecipes()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Подобрать рецепты (${products.size})") }
            TextButton(onClick = { vm.update { it.copy(products = emptyList()) } }) { Text("Очистить список") }
        }
    }
}

// ---------- Рецепты ----------

@Composable
fun RecipesScreen(vm: AppViewModel) {
    val addToDiary = rememberDiaryAdder(vm)
    Screen {
        SectionTitle("Рецепты из того, что есть")
        Stepper("Количество порций", vm.data.servings, 1..12) { n -> vm.update { it.copy(servings = n) } }
        Button(onClick = { vm.findRecipes() }, modifier = Modifier.fillMaxWidth()) {
            Text("По списку продуктов (${vm.data.products.size})")
        }
        Hint("Или сразу по фото продуктов:")
        PhotoButtons { vm.findRecipes(it) }

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
        SectionTitle("Рацион")
        Segmented(listOf("На 1 день", "На неделю"), period) { period = it }
        Stepper("Количество человек", vm.data.people, 1..12) { n -> vm.update { it.copy(people = n) } }
        SwitchRow("Использовать мои продукты (${vm.data.products.size})", useProducts) { useProducts = it }
        SwitchRow("Учитывать мою норму: ${target.kcal.r()} ккал на человека", useTarget) { useTarget = it }
        OutlinedTextField(
            value = wishes,
            onValueChange = { wishes = it },
            label = { Text("Пожелания (необязательно)") },
            placeholder = { Text("без свинины, побольше рыбы, бюджетно…") },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { vm.makePlan(if (period == 0) 1 else 7, useProducts, useTarget, wishes) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Составить рацион") }

        val plan = vm.data.plan ?: return@Screen
        Hint("Рацион рассчитан на ${plan.people} чел. КБЖУ указаны на одну порцию.")
        plan.days.forEach { day ->
            Column {
                SectionTitle(day.title)
                Hint(
                    "Итого на человека: " + macroText(
                        day.meals.sumOf { it.kcal },
                        day.meals.sumOf { it.protein },
                        day.meals.sumOf { it.fat },
                        day.meals.sumOf { it.carbs },
                    )
                )
            }
            day.meals.forEach { RecipeCard(it, addToDiary) }
        }
        if (plan.shoppingList.isNotEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SectionTitle("Список покупок")
                    plan.shoppingList.forEach {
                        Text("• ${it.name}" + if (it.amount.isNotBlank()) " — ${it.amount}" else "")
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
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

private fun String.num(): Double = replace(',', '.').toDoubleOrNull() ?: 0.0

@Composable
fun CaloriesScreen(vm: AppViewModel) {
    val profile = vm.data.profile
    var age by rememberSaveable { mutableStateOf(profile.age.toString()) }
    var height by rememberSaveable { mutableStateOf(profile.heightCm.toString()) }
    var weight by rememberSaveable { mutableStateOf(profile.weightKg.toString().removeSuffix(".0")) }
    var activityOpen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.rollDiary() }

    Screen {
        SectionTitle("Моя норма калорий")
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
                Text("Активность: " + Nutrition.activities[profile.activity.coerceIn(Nutrition.activities.indices)].first)
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

        val target = Nutrition.target(profile)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "${target.kcal.r()} ккал в сутки",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text("Белки ${target.protein.r()} г · Жиры ${target.fat.r()} г · Углеводы ${target.carbs.r()} г")
                Hint("Основной обмен ${Nutrition.bmr(profile).r()} ккал, расход с активностью ${Nutrition.tdee(profile).r()} ккал (формула Миффлина — Сан-Жеора). Расчёт ориентировочный и не заменяет консультацию врача.")
            }
        }

        // ----- дневник -----
        val diary = vm.data.diary
        val eaten = diary.sumOf { it.kcal }
        SectionTitle("Съедено сегодня")
        LinearProgressIndicator(
            progress = { (eaten / target.kcal).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )
        val left = target.kcal - eaten
        Text(
            "${eaten.r()} из ${target.kcal.r()} ккал · " +
                if (left >= 0) "осталось ${left.r()}" else "перебор ${(-left).r()}"
        )
        Hint(
            "Б ${diary.sumOf { it.protein }.r()} / ${target.protein.r()} · " +
                "Ж ${diary.sumOf { it.fat }.r()} / ${target.fat.r()} · " +
                "У ${diary.sumOf { it.carbs }.r()} / ${target.carbs.r()} г"
        )
        diary.forEach { entry ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(entry.name)
                    Hint(macroText(entry.kcal, entry.protein, entry.fat, entry.carbs))
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

    SectionTitle("Добавить приём пищи")
    OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Что съели") },
        placeholder = { Text("тарелка борща и 2 куска хлеба") },
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedButton(onClick = { vm.estimateDish(name) }, modifier = Modifier.fillMaxWidth()) {
        Text("Оценить калории по описанию")
    }
    Hint("Или по фото блюда:")
    PhotoButtons { vm.estimateDish(name, it) }
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
    ) { Text("Добавить в дневник") }
}
