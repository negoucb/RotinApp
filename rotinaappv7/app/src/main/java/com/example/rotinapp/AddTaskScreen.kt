package com.example.rotinapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.rotinapp.database.RecurrenceType
import com.example.rotinapp.database.TaskEntity
import com.example.rotinapp.database.TaskViewModel
import kotlinx.coroutines.launch

private const val TITLE_MAX = 50
private const val DESC_MAX = 200

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(
    navController: NavController,
    viewModel: TaskViewModel,
    category: String,
    isDarkMode: Boolean = false,
    notificationsEnabled: Boolean = true,
    existingTask: TaskEntity? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf(existingTask?.title ?: "") }
    var description by remember { mutableStateOf(existingTask?.description ?: "") }
    var selectedHour by remember {
        val h = existingTask?.time?.split(":")?.getOrNull(0)?.toIntOrNull() ?: 8
        mutableStateOf(h)
    }
    var selectedMinute by remember {
        val m = existingTask?.time?.split(":")?.getOrNull(1)?.toIntOrNull() ?: 0
        mutableStateOf(m)
    }
    var showTimePicker by remember { mutableStateOf(false) }
    var recurrenceType by remember {
        mutableStateOf(
            RecurrenceType.values().firstOrNull { it.name == existingTask?.recurrenceType }
                ?: RecurrenceType.DAILY
        )
    }

    val weekDays = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")
    val selectedWeekDays = remember {
        val days = existingTask?.recurrenceDays
            ?.split(",")?.mapNotNull { it.trim().toIntOrNull() } ?: emptyList()
        mutableStateListOf<Int>().also { it.addAll(days) }
    }
    val selectedMonthDays = remember {
        val days = existingTask?.recurrenceDays
            ?.split(",")?.mapNotNull { it.trim().toIntOrNull() } ?: emptyList()
        mutableStateListOf<Int>().also { it.addAll(days) }
    }

    val timeString = "%02d:%02d".format(selectedHour, selectedMinute)
    val isEditing = existingTask != null

    val gradientColors = if (isDarkMode)
        listOf(Color(0xFF1A1A1A), Color(0xFF2D2D2D))
    else
        listOf(Color(0xFFE0E0E0), Color(0xFF757575))
    val textColor = if (isDarkMode) Color.White else Color.Black
    val cardColor = if (isDarkMode) Color(0xFF2D2D2D) else Color.White

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(gradientColors))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = textColor)
                }
                Text(
                    text = if (isEditing) "Editar tarefa — $category" else "Nova tarefa — $category",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }

            // ── Título com contador ──────────────────────────────────────────
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= TITLE_MAX) title = it },
                    label = { Text("Nome da atividade") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = cardColor,
                        unfocusedContainerColor = cardColor,
                        focusedLabelColor = textColor,
                        unfocusedLabelColor = textColor.copy(alpha = 0.6f),
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor
                    )
                )
                Text(
                    text = "${title.length}/$TITLE_MAX",
                    fontSize = 11.sp,
                    color = if (title.length >= TITLE_MAX) Color(0xFFD32F2F) else textColor.copy(alpha = 0.4f),
                    modifier = Modifier.align(Alignment.End).padding(top = 2.dp, end = 4.dp)
                )
            }

            // ── Descrição com contador ───────────────────────────────────────
            Column {
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= DESC_MAX) description = it },
                    label = { Text("Descrição (opcional)") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = cardColor,
                        unfocusedContainerColor = cardColor,
                        focusedLabelColor = textColor,
                        unfocusedLabelColor = textColor.copy(alpha = 0.6f),
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor
                    )
                )
                Text(
                    text = "${description.length}/$DESC_MAX",
                    fontSize = 11.sp,
                    color = if (description.length >= DESC_MAX) Color(0xFFD32F2F) else textColor.copy(alpha = 0.4f),
                    modifier = Modifier.align(Alignment.End).padding(top = 2.dp, end = 4.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = cardColor,
                modifier = Modifier.fillMaxWidth().clickable { showTimePicker = true }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Horário", fontSize = 16.sp, color = textColor.copy(alpha = 0.7f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = textColor.copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(timeString, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
                    }
                }
            }

            Text("Repetir", fontWeight = FontWeight.Medium, color = textColor, fontSize = 16.sp)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    RecurrenceType.DAILY to "Todo dia",
                    RecurrenceType.WEEKLY to "Dias da semana",
                    RecurrenceType.MONTHLY to "Dias do mês"
                ).forEach { (type, label) ->
                    FilterChip(
                        selected = recurrenceType == type,
                        onClick = { recurrenceType = type },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (isDarkMode) Color.White else Color.Black,
                            selectedLabelColor = if (isDarkMode) Color.Black else Color.White,
                            containerColor = cardColor,
                            labelColor = textColor
                        )
                    )
                }
            }

            if (recurrenceType == RecurrenceType.WEEKLY) {
                Text("Selecione os dias:", fontSize = 14.sp, color = textColor.copy(alpha = 0.7f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    weekDays.forEachIndexed { index, day ->
                        val dayNum = index + 1
                        val selected = selectedWeekDays.contains(dayNum)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (selected) (if (isDarkMode) Color.White else Color.Black) else cardColor,
                                    CircleShape
                                )
                                .clickable {
                                    if (selected) selectedWeekDays.remove(dayNum)
                                    else selectedWeekDays.add(dayNum)
                                }
                        ) {
                            Text(
                                day, fontSize = 11.sp,
                                color = if (selected) (if (isDarkMode) Color.Black else Color.White) else textColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            if (recurrenceType == RecurrenceType.MONTHLY) {
                Text("Selecione os dias do mês:", fontSize = 14.sp, color = textColor.copy(alpha = 0.7f))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..31).chunked(7).forEach { week ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            week.forEach { day ->
                                val selected = selectedMonthDays.contains(day)
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(
                                            if (selected) (if (isDarkMode) Color.White else Color.Black) else cardColor,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(1.dp, if (selected) Color.Transparent else textColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        .clickable {
                                            if (selected) selectedMonthDays.remove(day)
                                            else selectedMonthDays.add(day)
                                        }
                                ) {
                                    Text(
                                        "$day", fontSize = 12.sp,
                                        color = if (selected) (if (isDarkMode) Color.Black else Color.White) else textColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val days = when (recurrenceType) {
                            RecurrenceType.WEEKLY -> selectedWeekDays.sorted().joinToString(",")
                            RecurrenceType.MONTHLY -> selectedMonthDays.sorted().joinToString(",")
                            else -> ""
                        }
                        scope.launch {
                            if (isEditing && existingTask != null) {
                                cancelTaskNotification(context, existingTask.id)
                                val updated = existingTask.copy(
                                    title = title,
                                    time = timeString,
                                    description = description,
                                    recurrenceType = recurrenceType.name,
                                    recurrenceDays = days
                                )
                                viewModel.update(updated)
                                if (notificationsEnabled) {
                                    scheduleTaskNotification(context, updated)
                                }
                            } else {
                                val savedTask = viewModel.insertAndGetId(
                                    title = title,
                                    time = timeString,
                                    description = description,
                                    category = category,
                                    recurrenceType = recurrenceType.name,
                                    recurrenceDays = days
                                )
                                if (notificationsEnabled) {
                                    scheduleTaskNotification(context, savedTask)
                                }

                                // Navegação entre Activities via Intent, com parâmetro (requisito do professor)
                                val intent = Intent(context, ConfirmacaoTarefaActivity::class.java)
                                intent.putExtra(ConfirmacaoTarefaActivity.EXTRA_NOME_TAREFA, title)
                                intent.putExtra(ConfirmacaoTarefaActivity.EXTRA_HORARIO, timeString)
                                context.startActivity(intent)
                            }
                            navController.popBackStack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDarkMode) Color.White else Color.Black,
                    contentColor = if (isDarkMode) Color.Black else Color.White
                )
            ) {
                Text(if (isEditing) "Salvar alterações" else "Salvar tarefa", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedHour,
            initialMinute = selectedMinute
        )

        val accentColor  = if (isDarkMode) Color.White else Color.Black
        val onAccent     = if (isDarkMode) Color.Black else Color.White
        val dialogBg     = if (isDarkMode) Color(0xFF2D2D2D) else Color.White

        androidx.compose.ui.window.Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = dialogBg,
                tonalElevation = 0.dp,
                modifier = Modifier.wrapContentSize()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Escolha o horário",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    TimePicker(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            clockDialColor          = if (isDarkMode) Color(0xFF3A3A3A) else Color(0xFFF0F0F0),
                            clockDialSelectedContentColor  = onAccent,
                            clockDialUnselectedContentColor = textColor,
                            selectorColor           = accentColor,
                            containerColor          = dialogBg,
                            timeSelectorSelectedContainerColor   = accentColor,
                            timeSelectorUnselectedContainerColor = if (isDarkMode) Color(0xFF3A3A3A) else Color(0xFFF0F0F0),
                            timeSelectorSelectedContentColor     = onAccent,
                            timeSelectorUnselectedContentColor   = textColor,
                            periodSelectorSelectedContainerColor = accentColor,
                            periodSelectorUnselectedContainerColor = if (isDarkMode) Color(0xFF3A3A3A) else Color(0xFFF0F0F0),
                            periodSelectorSelectedContentColor   = onAccent,
                            periodSelectorUnselectedContentColor = textColor,
                            periodSelectorBorderColor            = accentColor.copy(alpha = 0.3f)
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showTimePicker = false },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
                        ) {
                            Text("Cancelar", color = textColor, fontSize = 15.sp)
                        }
                        Button(
                            onClick = {
                                selectedHour   = timePickerState.hour
                                selectedMinute = timePickerState.minute
                                showTimePicker = false
                            },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor   = onAccent
                            )
                        ) {
                            Text("Confirmar", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}