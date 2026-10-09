@file:OptIn(ExperimentalMaterial3Api::class)

package app.mealmate

import android.graphics.Color.TRANSPARENT
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.mealmate.ui.AppTheme
import app.mealmate.ui.CaloriesScreen
import app.mealmate.ui.Hint
import app.mealmate.ui.PlanScreen
import app.mealmate.ui.ProductsScreen
import app.mealmate.ui.RecipesScreen
import app.mealmate.ui.headerBrush

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // шапка всегда тёмно-зелёная, поэтому значки статус-бара светлые
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(TRANSPARENT))
        setContent { AppTheme { App() } }
    }
}

private val tabs = listOf(
    "Продукты" to R.drawable.ic_fridge,
    "Рецепты" to R.drawable.ic_restaurant,
    "Рацион" to R.drawable.ic_calendar,
    "Калории" to R.drawable.ic_fire,
)

@Composable
fun App(startTab: Int = 0, vm: AppViewModel = viewModel()) {
    var tab by rememberSaveable { mutableIntStateOf(startTab) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Box(
                Modifier
                    .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                    .background(headerBrush())
            ) {
                TopAppBar(
                    title = { Text(tabs[tab].first, style = MaterialTheme.typography.headlineMedium) },
                    actions = {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Настройки")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White,
                    ),
                )
            }
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, (title, icon) ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(painterResource(icon), contentDescription = null) },
                        label = { Text(title) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
            when (tab) {
                0 -> ProductsScreen(vm, onShowRecipes = { tab = 1 })
                1 -> RecipesScreen(vm)
                2 -> PlanScreen(vm)
                else -> CaloriesScreen(vm)
            }
        }
    }

    if (showSettings) SettingsDialog(vm) { showSettings = false }

    vm.busy?.let { message ->
        AlertDialog(
            onDismissRequest = {},
            confirmButton = { TextButton(onClick = { vm.cancel() }) { Text("Отмена") } },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator()
                    Text(message)
                }
            },
        )
    }

    vm.error?.let { message ->
        AlertDialog(
            onDismissRequest = { vm.error = null },
            confirmButton = { TextButton(onClick = { vm.error = null }) { Text("Понятно") } },
            text = { Text(message) },
        )
    }
}

@Composable
private fun SettingsDialog(vm: AppViewModel, onClose: () -> Unit) {
    var key by rememberSaveable { mutableStateOf(vm.data.apiKey) }
    var model by rememberSaveable { mutableStateOf(vm.data.model) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Настройки") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it.trim() },
                    label = { Text("Ключ Gemini API") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Hint("Получить: aistudio.google.com/apikey")
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it.trim() },
                    label = { Text("Модель") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                vm.update { it.copy(apiKey = key, model = model.ifEmpty { DEFAULT_MODEL }) }
                onClose()
            }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Отмена") } },
    )
}
