package com.mrwind.lunar.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class WeatherResult(
    val tempText: String,
    val descText: String,
    val weatherCode: Int
)

object WeatherFetcher {
    suspend fun fetchWeather(lat: String, lon: String): WeatherResult? = withContext(Dispatchers.IO) {
        return@withContext try {
            val cleanLat = lat.trim().ifEmpty { "11.2271" }
            val cleanLon = lon.trim().ifEmpty { "107.6671" }
            val urlStr =
                "https://api.open-meteo.com/v1/forecast?latitude=$cleanLat&longitude=$cleanLon&current=temperature_2m,weather_code&timezone=Asia%2FBangkok"
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
            }
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val current = json.optJSONObject("current")
                val temp = current?.optDouble("temperature_2m", Double.NaN) ?: Double.NaN
                val code = current?.optInt("weather_code", -1) ?: -1
                conn.disconnect()
                if (!temp.isNaN()) {
                    WeatherResult(
                        tempText = "${Math.round(temp)}°C",
                        descText = mapWeatherCode(code),
                        weatherCode = code
                    )
                } else null
            } else {
                conn.disconnect()
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun mapWeatherCode(code: Int): String {
        return when (code) {
            0 -> "Trời quang"
            1, 2 -> "Ít mây"
            3 -> "Nhiều mây"
            45, 48 -> "Sương mù"
            51, 53, 55 -> "Mưa phùn"
            61, 63, 65 -> "Có mưa"
            80, 81, 82 -> "Mưa rào"
            95, 96, 99 -> "Giông bão"
            else -> "Trời quang"
        }
    }
}
