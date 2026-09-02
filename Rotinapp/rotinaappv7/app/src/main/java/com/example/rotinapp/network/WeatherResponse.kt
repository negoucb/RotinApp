package com.example.rotinapp.network

data class WeatherResponse(
    val current_weather: CurrentWeather,
    val hourly: HourlyWeather
)

data class CurrentWeather(
    val temperature: Double,
    val windspeed: Double,
    val time: String,
    val weathercode: Int
)

data class HourlyWeather(
    val time: List<String>,
    val temperature_2m: List<Double>,
    val weathercode: List<Int>,
    val precipitation_probability: List<Int>
)