package com.example.rotinapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.rotinapp.network.RetrofitInstance
import com.example.rotinapp.network.WeatherResponse
import kotlinx.coroutines.launch

private const val BRASILIA_LAT = -15.78
private const val BRASILIA_LON = -47.93

@Composable
fun ApiScreen(navController: NavController, isDarkMode: Boolean = false) {
    var weather by remember { mutableStateOf<WeatherResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error   by remember { mutableStateOf("") }
    val scope   = rememberCoroutineScope()

    val gradientColors = if (isDarkMode)
        listOf(Color(0xFF1A1A1A), Color(0xFF2D2D2D))
    else
        listOf(Color(0xFFE0E0E0), Color(0xFF757575))
    val textColor  = if (isDarkMode) Color.White else Color.Black
    val cardColor  = if (isDarkMode) Color(0xFF2D2D2D) else Color.White
    val subText    = textColor.copy(alpha = 0.6f)

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                weather = RetrofitInstance.api.getWeather(
                    latitude  = BRASILIA_LAT,
                    longitude = BRASILIA_LON
                )
            } catch (e: Exception) {
                error = e.message ?: "Erro ao carregar clima"
            } finally {
                loading = false
            }
        }
    }

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
            // ── Top bar ──────────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = textColor)
                }
                Text(
                    "Clima — Brasília",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }

            when {
                loading -> {
                    Box(Modifier.fillMaxWidth().padding(top = 64.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = textColor)
                    }
                }
                error.isNotEmpty() -> {
                    Text("Erro: $error", color = Color(0xFFD32F2F))
                }
                weather != null -> {
                    val w = weather!!
                    val (icon, label) = mapearCodigoClima(w.current_weather.weathercode)

                    // ── Card atual ───────────────────────────────────────────
                    Surface(shape = RoundedCornerShape(16.dp), color = cardColor, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Agora", fontSize = 13.sp, color = subText, fontWeight = FontWeight.Medium)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(40.dp))
                                Column {
                                    Text(
                                        "${w.current_weather.temperature}°C",
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                    Text(label, fontSize = 14.sp, color = subText)
                                }
                            }
                            Divider(color = textColor.copy(alpha = 0.08f))
                            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                InfoItem("💨 Vento", "${w.current_weather.windspeed} km/h", textColor, subText)
                                InfoItem("🕐 Atualizado", w.current_weather.time.substringAfter("T").take(5), textColor, subText)
                            }
                        }
                    }

                    // ── Previsão horária ─────────────────────────────────────
                    Text(
                        "Previsão horária — hoje",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = subText
                    )

                    // Pega as próximas 12 horas a partir da hora atual
                    val currentHour = w.current_weather.time.substringAfter("T").take(2).toIntOrNull() ?: 0
                    val hourlySlots = w.hourly.time.indices
                        .filter {
                            val h = w.hourly.time[it].substringAfter("T").take(2).toIntOrNull() ?: -1
                            h >= currentHour
                        }
                        .take(12)

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(hourlySlots) { i ->
                            val hora   = w.hourly.time[i].substringAfter("T").take(5)
                            val temp   = w.hourly.temperature_2m[i]
                            val code   = w.hourly.weathercode[i]
                            val prob   = w.hourly.precipitation_probability[i]
                            val (hIcon, _) = mapearCodigoClima(code)

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = cardColor,
                                modifier = Modifier.width(72.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(hora, fontSize = 12.sp, color = subText, fontWeight = FontWeight.Medium)
                                    Icon(hIcon, contentDescription = null, tint = textColor, modifier = Modifier.size(22.dp))
                                    Text(
                                        "${temp.toInt()}°",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                    if (prob > 0) {
                                        Text(
                                            "💧$prob%",
                                            fontSize = 10.sp,
                                            color = Color(0xFF64B5F6)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String, textColor: Color, subText: Color) {
    Column {
        Text(label, fontSize = 12.sp, color = subText)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textColor)
    }
}