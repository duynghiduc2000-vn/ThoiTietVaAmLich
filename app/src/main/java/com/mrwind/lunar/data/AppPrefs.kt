package com.mrwind.lunar.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TodoItem(
    val id: Long,
    val time: String,
    val title: String
)

data class AppConfig(
    val appThemeMode: String = "SYSTEM", // SYSTEM, LIGHT, DARK
    val colorThemeKey: String = "SKY_LIGHT",
    val locationName: String = "Nghị Đức",
    val latitude: String = "11.2271",
    val longitude: String = "107.6671",
    val signature: String = "",
    val lunarFontSizeSp: Float = 14f,
    val widgetStyle: String = "LIQUID_DARK", // TRANSPARENT, LIQUID_DARK, FROSTED_LIGHT, THEME_COLOR
    val showClockSeconds: Boolean = false,
    val showWeatherEffect: Boolean = true,
    val widgetSize: String = "4x2", // 4x2 or 4x4
    val tempDisplay: String = "28°C",
    val weatherDescDisplay: String = "Nhiều mây",
    val todos: List<TodoItem> = emptyList()
)

object AppPrefs {
    private const val PREF_NAME = "mrwind_lunar_prefs"

    private fun todayStr(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    fun loadConfig(context: Context): AppConfig {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val today = todayStr()
        val savedDate = sp.getString("last_todo_date", today) ?: today

        val todos = if (savedDate != today) {
            // Cuối ngày / sang ngày mới -> tự động xóa toàn bộ việc cần làm
            sp.edit()
                .putString("last_todo_date", today)
                .putString("todos_json", "[]")
                .apply()
            emptyList()
        } else {
            parseTodos(sp.getString("todos_json", "[]") ?: "[]")
        }

        return AppConfig(
            appThemeMode = sp.getString("app_theme_mode", "SYSTEM") ?: "SYSTEM",
            colorThemeKey = sp.getString("color_theme_key", "SKY_LIGHT") ?: "SKY_LIGHT",
            locationName = sp.getString("location_name", "Nghị Đức") ?: "Nghị Đức",
            latitude = sp.getString("latitude", "11.2271") ?: "11.2271",
            longitude = sp.getString("longitude", "107.6671") ?: "107.6671",
            signature = sp.getString("signature", "") ?: "",
            lunarFontSizeSp = sp.getFloat("lunar_font_size_sp", 14f),
            widgetStyle = sp.getString("widget_style", "LIQUID_DARK") ?: "LIQUID_DARK",
            showClockSeconds = sp.getBoolean("show_clock_seconds", false),
            showWeatherEffect = sp.getBoolean("show_weather_effect", true),
            widgetSize = sp.getString("widget_size", "4x2") ?: "4x2",
            tempDisplay = sp.getString("temp_display", "28°C") ?: "28°C",
            weatherDescDisplay = sp.getString("weather_desc_display", "Nhiều mây") ?: "Nhiều mây",
            todos = todos
        )
    }

    fun saveConfig(context: Context, config: AppConfig) {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sp.edit()
            .putString("last_todo_date", todayStr())
            .putString("app_theme_mode", config.appThemeMode)
            .putString("color_theme_key", config.colorThemeKey)
            .putString("location_name", config.locationName)
            .putString("latitude", config.latitude)
            .putString("longitude", config.longitude)
            .putString("signature", config.signature)
            .putFloat("lunar_font_size_sp", config.lunarFontSizeSp)
            .putString("widget_style", config.widgetStyle)
            .putBoolean("show_clock_seconds", config.showClockSeconds)
            .putBoolean("show_weather_effect", config.showWeatherEffect)
            .putString("widget_size", config.widgetSize)
            .putString("temp_display", config.tempDisplay)
            .putString("weather_desc_display", config.weatherDescDisplay)
            .putString("todos_json", encodeTodos(config.todos.take(10)))
            .apply()
    }

    private fun parseTodos(raw: String): List<TodoItem> {
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<TodoItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    TodoItem(
                        id = obj.optLong("id", System.currentTimeMillis() + i),
                        time = obj.optString("time", "08:00"),
                        title = obj.optString("title", "")
                    )
                )
            }
            list.take(10)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun encodeTodos(list: List<TodoItem>): String {
        val arr = JSONArray()
        for (item in list.take(10)) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("time", item.time)
            obj.put("title", item.title)
            arr.put(obj)
        }
        return arr.toString()
    }
}
