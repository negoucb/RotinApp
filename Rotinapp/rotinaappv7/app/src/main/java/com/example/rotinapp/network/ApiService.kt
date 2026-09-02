package com.example.rotinapp.network

import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    @GET("v1/forecast")
    suspend fun getWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current_weather") currentWeather: Boolean = true,
        @Query("hourly") hourly: String = "temperature_2m,weathercode,precipitation_probability",
        @Query("forecast_days") forecastDays: Int = 1,
        @Query("timezone") timezone: String = "America/Sao_Paulo"
    ): WeatherResponse
}