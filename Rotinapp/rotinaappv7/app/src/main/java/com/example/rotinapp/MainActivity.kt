package com.example.rotinapp

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.rotinapp.database.TaskEntity
import com.example.rotinapp.database.TaskViewModel
import com.example.rotinapp.network.RetrofitInstance
import com.example.rotinapp.network.WeatherResponse
import com.example.rotinapp.ui.theme.RotinappTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { granted ->
            // Permissão concedida ou negada – canal já criado, alarms continuam funcionando
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel(this)

        // Android 13+ (TIRAMISU): solicita permissão de exibir notificações
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Android 12+ (S): garante permissão de alarme exato
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(android.app.AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                openExactAlarmPermissionSettings(this)
            }
        }

        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val settingsManager = remember { SettingsManager(context) }
            val isDarkMode by settingsManager.isDarkMode.collectAsState(initial = false)
            val notificationsEnabled by settingsManager.notificationsEnabled.collectAsState(initial = true)
            val scope = rememberCoroutineScope()

            RotinappTheme(darkTheme = isDarkMode) {
                val navController = rememberNavController()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SetupNavGraph(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding),
                        isDarkMode = isDarkMode,
                        notificationsEnabled = notificationsEnabled,
                        onToggleDarkMode = { scope.launch { settingsManager.setDarkMode(it) } },
                        onToggleNotifications = { enabled ->
                            scope.launch { settingsManager.setNotificationsEnabled(enabled) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun horarioBsb(): Pair<String, String> {
    var time by remember { mutableStateOf(horarioBsbAtual()) }
    LaunchedEffect(Unit) {
        while (true) { delay(1000); time = horarioBsbAtual() }
    }
    return time
}

fun horarioBsbAtual(): Pair<String, String> {
    val tz = TimeZone.getTimeZone("America/Sao_Paulo")
    val cal = Calendar.getInstance(tz)
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault()).also { it.timeZone = tz }
    val amPmFormat = SimpleDateFormat("a", Locale.US).also { it.timeZone = tz }
    return Pair(timeFormat.format(cal.time), amPmFormat.format(cal.time))
}

@Composable
fun getViewModel(): TaskViewModel {
    val context = LocalContext.current
    return viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory
            .getInstance(context.applicationContext as Application)
    )
}

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Trabalho : Screen("tela_trabalho")
    object Saude : Screen("tela_saude")
    object Lazer : Screen("tela_lazer")
    object Outras : Screen("tela_outras")
    object Api : Screen("api")
    object Settings : Screen("settings")
    object Detail : Screen("detail/{taskId}") {
        fun createRoute(taskId: Int) = "detail/$taskId"
    }
    object AddTask : Screen("add_task/{category}") {
        fun createRoute(category: String) = "add_task/$category"
    }
    object EditTask : Screen("edit_task/{taskId}") {
        fun createRoute(taskId: Int) = "edit_task/$taskId"
    }
}

@Composable
fun SetupNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean,
    notificationsEnabled: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    onToggleNotifications: (Boolean) -> Unit
) {
    NavHost(navController = navController, startDestination = Screen.Home.route, modifier = modifier) {
        composable(Screen.Home.route) { TelaHome(navController, isDarkMode) }
        composable(Screen.Trabalho.route) { TelaTrabalho(navController, isDarkMode) }
        composable(Screen.Saude.route) { TelaSaude(navController, isDarkMode) }
        composable(Screen.Lazer.route) { TelaLazer(navController, isDarkMode) }
        composable(Screen.Outras.route) { TelaOutras(navController, isDarkMode) }
        composable(Screen.Api.route) { ApiScreen(navController) }

        composable(Screen.Settings.route) {
            SettingsScreen(
                navController = navController,
                isDarkMode = isDarkMode,
                notificationsEnabled = notificationsEnabled,
                onToggleDarkMode = onToggleDarkMode,
                onToggleNotifications = onToggleNotifications
            )
        }

        composable(
            route = Screen.Detail.route,
            arguments = listOf(navArgument("taskId") { type = NavType.IntType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getInt("taskId") ?: -1
            DetailScreen(
                taskId = taskId,
                navController = navController,
                isDarkMode = isDarkMode,
                notificationsEnabled = notificationsEnabled
            )
        }

        composable(
            route = Screen.AddTask.route,
            arguments = listOf(navArgument("category") { type = NavType.StringType })
        ) { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category") ?: ""
            val viewModel = getViewModel()
            AddTaskScreen(
                navController = navController,
                viewModel = viewModel,
                category = category,
                isDarkMode = isDarkMode,
                notificationsEnabled = notificationsEnabled
            )
        }

        composable(
            route = Screen.EditTask.route,
            arguments = listOf(navArgument("taskId") { type = NavType.IntType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getInt("taskId") ?: -1
            val viewModel = getViewModel()
            var task by remember { mutableStateOf<TaskEntity?>(null) }
            LaunchedEffect(taskId) { task = viewModel.getTaskById(taskId) }

            task?.let { t ->
                AddTaskScreen(
                    navController = navController,
                    viewModel = viewModel,
                    category = t.category,
                    isDarkMode = isDarkMode,
                    notificationsEnabled = notificationsEnabled,
                    existingTask = t
                )

            }
        }
    }
}

// Helper para traduzir o código WMO do Open-Meteo para ícone e texto descritivo
fun mapearCodigoClima(code: Int): Pair<ImageVector, String> {
    return when (code) {
        0 -> Pair(Icons.Default.WbSunny, "Ensolarado")
        1, 2, 3 -> Pair(Icons.Default.CloudQueue, "Parcialmente Nublado")
        45, 48 -> Pair(Icons.Default.Dehaze, "Névoa") // Ícone corrigido aqui
        51, 53, 55, 61, 63, 65 -> Pair(Icons.Default.Umbrella, "Chovendo")
        71, 73, 75, 85, 86 -> Pair(Icons.Default.AcUnit, "Neve")
        80, 81, 82 -> Pair(Icons.Default.WaterDrop, "Pancadas de Chuva")
        95, 96, 99 -> Pair(Icons.Default.Thunderstorm, "Tempestade")
        else -> Pair(Icons.Default.Cloud, "Nublado")
    }
}

// Verifica se a condição climática inviabiliza ou atrapalha certas categorias
fun verificarInterferenciaClima(category: String, weatherCode: Int): Boolean {
    val categoriasAfetadas = listOf("Saude", "Lazer") // Ex: Corridas de rua, passeios, etc.
    val climaAdverso = weatherCode >= 51 // A partir de 51 na tabela WMO representa chuva/tempestade
    return categoriasAfetadas.contains(category) && climaAdverso
}

// ─── Tela Home ────────────────────────────────────────────────────────────────
@Composable
fun TelaHome(navController: NavController, isDarkMode: Boolean) {
    val (currentTime, currentAmPm) = horarioBsb()
    val viewModel = getViewModel()
    val allTasks by viewModel.allTasks.collectAsState(initial = emptyList())
    val pendingTasks = allTasks.filter { !it.isDone }.take(3)

    // Estados locais para controle de dados do Clima na Home
    var weatherData by remember { mutableStateOf<WeatherResponse?>(null) }
    var weatherError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            // Utilizando as coordenadas padrões da aplicação
            weatherData = RetrofitInstance.api.getWeather(latitude = -15.78, longitude = -47.93)
        } catch (e: Exception) {
            weatherError = true
        }
    }

    val gradientColors = if (isDarkMode) listOf(Color(0xFF1A1A1A), Color(0xFF2D2D2D))
    else listOf(Color(0xFFE0E0E0), Color(0xFF9E9E9E))
    val textColor = if (isDarkMode) Color.White else Color.Black
    val cardBgColor = if (isDarkMode) Color(0xFF2D2D2D) else Color.White

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(gradientColors))) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {

            // LISTA DE VALORES PARA TESTES DE CLIMAS:
            // Climas que NÃO geram alerta (Atividades seguras):
            // 0 → Ensolarado (Ícone de Sol)
            // 2 → Parcialmente Nublado (Ícone de Sol com Nuvem)
            // 45 → Névoa (Ícone de Neblina/Linhas)
            // 999 (ou qualquer outro não listado) → Nublado (Ícone de Nuvem Padrão)
            // Climas que GERAM alerta (Apenas em Saúde/Lazer):
            // 51 → Chovendo (Ícone de Guarda-chuva)
            // 71 → Neve (Ícone de Floco de Neve)
            // 80 → Pancadas de Chuva (Ícone de Gota D'água)
            // 95 → Tempestade (Ícone de Nuvem com Raio)
            // TESTE DE CLIMAS
            //val codigoClimaAtual = 95

            // Cabeçalho / Top Bar
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                // Card de Clima compacto integrado diretamente na Home
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cardBgColor,
                    modifier = Modifier
                        .width(160.dp)
                        .clickable { navController.navigate(Screen.Api.route) }
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        // TESTE DE CLIMAS:
                        //val (weatherIcon, weatherLabel) = mapearCodigoClima(codigoClimaAtual)
                        // ORIGINAIS:
                        val weatherCode = weatherData?.current_weather?.weathercode ?: 0
                        val (weatherIcon, weatherLabel) = mapearCodigoClima(weatherCode)

                        Icon(weatherIcon, contentDescription = null, tint = textColor, modifier = Modifier.size(24.dp))
                        Column {
                            Text(
                                text = weatherData?.let { "${it.current_weather.temperature}°C" } ?: "...",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            Text(weatherLabel, fontSize = 10.sp, color = textColor.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.navigate(Screen.Outras.route) }) {
                        Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = "Outras tarefas", modifier = Modifier.size(28.dp), tint = textColor)
                    }
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Configurações", modifier = Modifier.size(28.dp), tint = textColor)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Novo Componente de Avatar do Usuário Refatorado (Design Premium)
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(
                        brush = Brush.linearGradient(listOf(Color(0xFF6650a4), Color(0xFFEFB8C8))),
                        shape = CircleShape
                    )
                    .padding(3.dp)
                    .background(cardBgColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Avatar do Usuário",
                    modifier = Modifier.size(54.dp),
                    tint = textColor.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(currentTime, fontSize = 54.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                Text(currentAmPm, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor, modifier = Modifier.padding(bottom = 10.dp, start = 4.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Lista Dinâmica com os Cards atualizados de descrição e clima
            Text("Próximas Atividades", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.6f), modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp))

            if (pendingTasks.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    pendingTasks.forEach { task ->

                        // TESTE DE CLIMAS:
                        //val climaInterfere = verificarInterferenciaClima(task.category, codigoClimaAtual)
                        // ORIGINAL:
                        val climaInterfere = verificarInterferenciaClima(task.category, weatherData?.current_weather?.weathercode ?: 0)

                        HomeTaskItem(
                            task = task,
                            climaInterfere = climaInterfere,
                            isDarkMode = isDarkMode,
                            onClick = { navController.navigate(Screen.Detail.createRoute(task.id)) }
                        )
                    }
                }
            } else {
                Text("Nenhuma tarefa pendente 🎉", fontSize = 14.sp, color = textColor.copy(alpha = 0.7f), modifier = Modifier.padding(top = 16.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                NavCategoryButton(Color(0xFF8CFF9E), Icons.Default.FavoriteBorder) { navController.navigate(Screen.Saude.route) }
                NavCategoryButton(Color(0xFF8AB6FF), Icons.Default.Psychology) { navController.navigate(Screen.Lazer.route) }
                NavCategoryButton(Color(0xFFFF94B4), Icons.Default.Work) { navController.navigate(Screen.Trabalho.route) }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun HomeTaskItem(task: TaskEntity, climaInterfere: Boolean, isDarkMode: Boolean, onClick: () -> Unit) {
    val cardColor = if (isDarkMode) Color(0xFF2D2D2D) else Color.White
    val textColor = if (isDarkMode) Color.White else Color.Black

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardColor,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        if (task.isDone) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "${task.title} - ${task.time}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (climaInterfere) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Alerta de clima",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(20.dp).padding(end = 4.dp)
                        )
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = textColor.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                }
            }

            // Inclusão elegante da descrição expandida dentro do próprio Card
            if (task.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = task.description,
                    fontSize = 12.sp,
                    color = textColor.copy(alpha = 0.6f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 30.dp)
                )
            }

            // Mensagem dinâmica contextual do aviso de interferência meteorológica
            if (climaInterfere) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFB300).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp).padding(start = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚠️ Condições do clima podem afetar esta atividade externa.",
                        fontSize = 10.sp,
                        color = if (isDarkMode) Color(0xFFFFE082) else Color(0xFF7F5F00),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun NavCategoryButton(bgColor: Color, icon: ImageVector, onClick: () -> Unit) {
    Surface(shape = CircleShape, color = Color.White, modifier = Modifier.size(72.dp).clickable { onClick() }) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.size(56.dp).background(bgColor, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Color.Black, modifier = Modifier.size(26.dp))
            }
        }
    }
}

// ─── Telas de categoria ────────────────────────────────────────────────────────
@Composable
fun TelaTrabalho(navController: NavController, isDarkMode: Boolean) {
    val viewModel = getViewModel()
    val allTasks by viewModel.allTasks.collectAsState(initial = emptyList())
    val (currentTime, currentAmPm) = horarioBsb()
    CategoriasTasks(
        categoryTitle = "Trabalho", timeDisplay = currentTime, amPm = currentAmPm,
        icon = Icons.Default.Work,
        gradientColors = if (isDarkMode) listOf(Color(0xFF1A1A1A), Color(0xFF2D2D2D)) else listOf(Color(0xFFEFEFEF), Color(0xFFFF94B4)),
        taskList = allTasks.filter { it.category == "Trabalho" }, isDarkMode = isDarkMode,
        onTaskClick = { task -> navController.navigate(Screen.Detail.createRoute(task.id)) },
        onAddTaskClick = { navController.navigate(Screen.AddTask.createRoute("Trabalho")) },
        onBackClick = { navController.popBackStack() }
    )
}

@Composable
fun TelaSaude(navController: NavController, isDarkMode: Boolean) {
    val viewModel = getViewModel()
    val allTasks by viewModel.allTasks.collectAsState(initial = emptyList())
    val (currentTime, currentAmPm) = horarioBsb()
    CategoriasTasks(
        categoryTitle = "Saúde e bem-estar", timeDisplay = currentTime, amPm = currentAmPm,
        icon = Icons.Default.FavoriteBorder,
        gradientColors = if (isDarkMode) listOf(Color(0xFF1A1A1A), Color(0xFF2D2D2D)) else listOf(Color(0xFFEFEFEF), Color(0xFF8CFF9E)),
        taskList = allTasks.filter { it.category == "Saude" }, isDarkMode = isDarkMode,
        onTaskClick = { task -> navController.navigate(Screen.Detail.createRoute(task.id)) },
        onAddTaskClick = { navController.navigate(Screen.AddTask.createRoute("Saude")) },
        onBackClick = { navController.popBackStack() }
    )
}

@Composable
fun TelaLazer(navController: NavController, isDarkMode: Boolean) {
    val viewModel = getViewModel()
    val allTasks by viewModel.allTasks.collectAsState(initial = emptyList())
    val (currentTime, currentAmPm) = horarioBsb()
    CategoriasTasks(
        categoryTitle = "Lazer e aprendizado", timeDisplay = currentTime, amPm = currentAmPm,
        icon = Icons.Default.Lightbulb,
        gradientColors = if (isDarkMode) listOf(Color(0xFF1A1A1A), Color(0xFF2D2D2D)) else listOf(Color(0xFFEFEFEF), Color(0xFF8AB6FF)),
        taskList = allTasks.filter { it.category == "Lazer" }, isDarkMode = isDarkMode,
        onTaskClick = { task -> navController.navigate(Screen.Detail.createRoute(task.id)) },
        onAddTaskClick = { navController.navigate(Screen.AddTask.createRoute("Lazer")) },
        onBackClick = { navController.popBackStack() }
    )
}

@Composable
fun TelaOutras(navController: NavController, isDarkMode: Boolean) {
    val viewModel = getViewModel()
    val allTasks by viewModel.allTasks.collectAsState(initial = emptyList())
    val (currentTime, currentAmPm) = horarioBsb()
    CategoriasTasks(
        categoryTitle = "Outras tarefas", timeDisplay = currentTime, amPm = currentAmPm,
        icon = Icons.AutoMirrored.Filled.Assignment,
        gradientColors = if (isDarkMode) listOf(Color(0xFF1A1A1A), Color(0xFF2D2D2D)) else listOf(Color(0xFFEFEFEF), Color(0xFF9E9E9E)),
        taskList = allTasks.filter { it.category == "Outras" }, isDarkMode = isDarkMode,
        onTaskClick = { task -> navController.navigate(Screen.Detail.createRoute(task.id)) },
        onAddTaskClick = { navController.navigate(Screen.AddTask.createRoute("Outras")) },
        onBackClick = { navController.popBackStack() }
    )
}

// ─── Detail Screen ─────────────────────────────────────────────────────────────
@Composable
fun DetailScreen(taskId: Int, navController: NavController, isDarkMode: Boolean, notificationsEnabled: Boolean) {
    val viewModel = getViewModel()
    val context = LocalContext.current
    var task by remember { mutableStateOf<TaskEntity?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    LaunchedEffect(taskId) { task = viewModel.getTaskById(taskId) }

    val gradientColors = if (isDarkMode) listOf(Color(0xFF1A1A1A), Color(0xFF2D2D2D))
    else listOf(Color(0xFFE0E0E0), Color(0xFF757575))
    val textColor = if (isDarkMode) Color.White else Color.Black
    val cardColor = if (isDarkMode) Color(0xFF2D2D2D) else Color.White

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(gradientColors))) {

        if (task != null) {
            val t = task!!

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Botão de voltar como primeiro item — garante que não fica coberto pela lista
                item {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = textColor)
                    }
                }

                item {
                    Text(t.title, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = textColor)
                }

                item {
                    Surface(shape = RoundedCornerShape(12.dp), color = cardColor, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (t.isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null, tint = if (t.isDone) Color(0xFF4CAF50) else textColor.copy(alpha = 0.5f),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(if (t.isDone) "Concluída" else "Pendente", fontSize = 15.sp, color = textColor)
                            }
                            TextButton(onClick = {
                                val updated = t.copy(isDone = !t.isDone)
                                viewModel.update(updated)
                                task = updated
                            }) {
                                Text(if (t.isDone) "Reabrir" else "Concluir", color = if (isDarkMode) Color.White else Color.Black)
                            }
                        }
                    }
                }

                item {
                    // Remapeia o nome interno da categoria para exibição limpa ao usuário
                    val visualCategoryName = when(t.category) {
                        "Saude" -> "Saúde e bem-estar"
                        "Lazer" -> "Lazer e aprendizado"
                        else -> t.category
                    }
                    DetailInfoCard(icon = Icons.Default.Folder, label = "Categoria", value = visualCategoryName, cardColor = cardColor, textColor = textColor)
                }

                item {
                    DetailInfoCard(icon = Icons.Default.Schedule, label = "Horário", value = t.time, cardColor = cardColor, textColor = textColor)
                }

                if (t.description.isNotBlank()) {
                    item {
                        DetailInfoCard(icon = Icons.Default.Notes, label = "Descrição", value = t.description, cardColor = cardColor, textColor = textColor)
                    }
                }

                item {
                    val recorrencia = when (t.recurrenceType) {
                        "WEEKLY" -> {
                            val nomeDias = mapOf(1 to "Seg", 2 to "Ter", 3 to "Qua", 4 to "Qui", 5 to "Sex", 6 to "Sáb", 7 to "Dom")
                            val dias = t.recurrenceDays.split(",").mapNotNull { it.trim().toIntOrNull() }
                                .mapNotNull { nomeDias[it] }.joinToString(", ")
                            "Semanal: $dias"
                        }
                        "MONTHLY" -> "Mensal: dias ${t.recurrenceDays}"
                        else -> "Todo dia"
                    }
                    DetailInfoCard(icon = Icons.Default.Repeat, label = "Recorrência", value = recorrencia, cardColor = cardColor, textColor = textColor)
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { navController.navigate(Screen.EditTask.createRoute(t.id)) },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDarkMode) Color.White else Color.Black,
                                contentColor = if (isDarkMode) Color.Black else Color.White
                            )
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Editar")
                        }

                        Button(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F), contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Excluir")
                        }
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = textColor)
                }
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tarefa não encontrada.", fontSize = 16.sp, color = textColor.copy(alpha = 0.7f))
                }
            }
        }
    }

    if (showDeleteDialog && task != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Excluir tarefa") },
            text = { Text("Tem certeza que deseja excluir \"${task!!.title}\"? Essa ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = {
                    cancelTaskNotification(context, task!!.id)
                    viewModel.delete(task!!)
                    showDeleteDialog = false
                    navController.popBackStack()
                }) { Text("Excluir", color = Color(0xFFD32F2F)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun DetailInfoCard(icon: ImageVector, label: String, value: String, cardColor: Color, textColor: Color) {
    Surface(shape = RoundedCornerShape(12.dp), color = cardColor, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, tint = textColor.copy(alpha = 0.6f), modifier = Modifier.size(20.dp).padding(top = 2.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(label, fontSize = 12.sp, color = textColor.copy(alpha = 0.5f))
                Text(value, fontSize = 15.sp, color = textColor, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ─── CategoriasTasks ──────────────────────────────────────────────────────────
@Composable
fun CategoriasTasks(
    categoryTitle: String, timeDisplay: String, amPm: String,
    icon: ImageVector, gradientColors: List<Color>,
    taskList: List<TaskEntity>, isDarkMode: Boolean,
    onTaskClick: (TaskEntity) -> Unit,
    onAddTaskClick: () -> Unit, onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor = if (isDarkMode) Color.White else Color.Black
    val cardColor = if (isDarkMode) Color(0xFF2D2D2D) else Color.White

    Box(modifier = modifier.fillMaxSize().background(Brush.verticalGradient(gradientColors))) {
        IconButton(onClick = onBackClick, modifier = Modifier.padding(16.dp).align(Alignment.TopStart)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = textColor)
        }
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(40.dp))
            Icon(icon, contentDescription = categoryTitle, tint = textColor, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(timeDisplay, fontSize = 48.sp, fontWeight = FontWeight.Bold, color = textColor)
                Text(amPm, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor, modifier = Modifier.padding(bottom = 8.dp, start = 4.dp))
            }
            Text(categoryTitle, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = textColor)
            Spacer(modifier = Modifier.height(32.dp))

            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(taskList) { task ->
                    ItemTaskRow(task = task, cardColor = cardColor, textColor = textColor, onClick = { onTaskClick(task) })
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 16.dp)) {
                FloatingActionButton(onClick = onAddTaskClick, containerColor = cardColor, shape = CircleShape, modifier = Modifier.size(64.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Adicionar tarefa", tint = textColor)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Adicionar tarefa", fontSize = 12.sp, color = textColor)
            }
        }
    }
}

@Composable
fun ItemTaskRow(task: TaskEntity, cardColor: Color, textColor: Color, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = cardColor,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${task.title} - ${task.time}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        fontSize = 11.sp,
                        color = textColor.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(if (task.isDone) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank, contentDescription = null, tint = textColor)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun TelaHomePreview() {
    RotinappTheme { TelaHome(rememberNavController(), false) }
}