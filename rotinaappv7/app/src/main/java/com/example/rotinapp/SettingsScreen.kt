package com.example.rotinapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController

@Composable
fun SettingsScreen(
    navController: NavController,
    isDarkMode: Boolean,
    notificationsEnabled: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    onToggleNotifications: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Verifica se a permissão de alarme exato está concedida (Android 12+)
    var hasExactAlarmPermission by remember {
        mutableStateOf(canScheduleExactAlarms(context))
    }

    // Reavalia quando o usuário volta das configurações do sistema
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            hasExactAlarmPermission = canScheduleExactAlarms(context)
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onToggleNotifications(true)
    }

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
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = textColor
                    )
                }
                Text(
                    text = "Configurações",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            SettingsToggleCard(
                title = "Modo escuro",
                subtitle = if (isDarkMode) "Tema escuro ativado" else "Tema claro ativado",
                icon = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                checked = isDarkMode,
                cardColor = cardColor,
                textColor = textColor,
                onCheckedChange = { onToggleDarkMode(it) }
            )

            SettingsToggleCard(
                title = "Notificações de tarefas",
                subtitle = if (notificationsEnabled)
                    "Você receberá lembretes no horário de cada tarefa"
                else
                    "Notificações desativadas",
                icon = if (notificationsEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                checked = notificationsEnabled,
                cardColor = cardColor,
                textColor = textColor,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val permission = Manifest.permission.POST_NOTIFICATIONS
                            val granted = ContextCompat.checkSelfPermission(context, permission) ==
                                    PackageManager.PERMISSION_GRANTED
                            if (!granted) {
                                notificationPermissionLauncher.launch(permission)
                                return@SettingsToggleCard
                            }
                        }
                        onToggleNotifications(true)
                    } else {
                        onToggleNotifications(false)
                    }
                }
            )

            // Aviso quando falta a permissão de alarme exato (Android 12+)
            if (notificationsEnabled && !hasExactAlarmPermission) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(20.dp).padding(top = 2.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Permissão de alarme necessária",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF7F3B00)
                            )
                            Text(
                                text = "Para receber notificações no horário exato, o Android 12+ exige que você autorize \"Alarmes e lembretes\" nas configurações do sistema.",
                                fontSize = 13.sp,
                                color = Color(0xFF7F3B00)
                            )
                            Button(
                                onClick = { openExactAlarmPermissionSettings(context) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE65100),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Conceder permissão", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            if (notificationsEnabled && hasExactAlarmPermission) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "💡 As notificações aparecem automaticamente no horário de cada tarefa cadastrada.",
                        fontSize = 13.sp,
                        color = textColor.copy(alpha = 0.7f),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsToggleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    cardColor: Color,
    textColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = textColor)
                    Text(subtitle, fontSize = 12.sp, color = textColor.copy(alpha = 0.6f))
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color.Black
                )
            )
        }
    }
}