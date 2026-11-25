package com.mrwind.lunar.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.Gravity
import android.widget.RemoteViews
import com.mrwind.lunar.MainActivity
import com.mrwind.lunar.R
import com.mrwind.lunar.data.AppConfig
import com.mrwind.lunar.data.AppPrefs
import com.mrwind.lunar.data.LunarCalendar
import java.util.Calendar
import java.util.TimeZone

object WidgetRenderer {

    fun colorHexForTheme(themeKey: String): Int {
        return when (themeKey) {
            "SKY_LIGHT" -> Color.parseColor("#1D4ED8")
            "MINT_LIGHT" -> Color.parseColor("#0F766E")
            "LEAF_GREEN" -> Color.parseColor("#15803D")
            "SUN_YELLOW" -> Color.parseColor("#B45309")
            "PEACH_ORANGE" -> Color.parseColor("#C2410C")
            "PINK_ROSE" -> Color.parseColor("#BE185D")
            else -> Color.parseColor("#252836")
        }
    }

    fun weatherIconRes(desc: String): Int {
        val lower = desc.lowercase()
        return when {
            "mưa" in lower || "rào" in lower || "phùn" in lower -> android.R.drawable.ic_menu_compass
            "mây" in lower || "sương" in lower -> android.R.drawable.ic_menu_view
            "giông" in lower || "bão" in lower -> android.R.drawable.ic_dialog_alert
            else -> android.R.drawable.ic_menu_day
        }
    }

    fun weatherEmoji(desc: String): String {
        val lower = desc.lowercase()
        return when {
            "giông" in lower || "bão" in lower -> "⛈️"
            "mưa" in lower || "rào" in lower || "phùn" in lower -> "🌧️"
            "sương" in lower -> "🌫️"
            "nhiều mây" in lower -> "☁️"
            "mây" in lower -> "⛅"
            else -> "☀️"
        }
    }

    fun updateAllWidgets(context: Context) {
        val config = AppPrefs.loadConfig(context)
        val mgr = AppWidgetManager.getInstance(context)

        val ids4x2 = mgr.getAppWidgetIds(ComponentName(context, LunarWeatherWidget4x2::class.java))
        for (id in ids4x2) {
            mgr.updateAppWidget(id, buildRemoteViews(context, config, is4x4 = false))
        }

        val ids4x4 = mgr.getAppWidgetIds(ComponentName(context, LunarWeatherWidget4x4::class.java))
        for (id in ids4x4) {
            mgr.updateAppWidget(id, buildRemoteViews(context, config, is4x4 = true))
        }
    }

    fun buildRemoteViews(context: Context, config: AppConfig, is4x4: Boolean): RemoteViews {
        val layoutId = if (is4x4) R.layout.widget_layout_4x4 else R.layout.widget_layout_4x2
        val views = RemoteViews(context.packageName, layoutId)

        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"))
        val solarText = LunarCalendar.formatSolarLine(cal)
        val lunarText = LunarCalendar.formatLunarLine(cal)

        val line2Text = if (config.todos.isNotEmpty()) {
            val minuteIndex = (System.currentTimeMillis() / 60_000L).toInt()
            val idx = Math.floorMod(minuteIndex, config.todos.size)
            val todo = config.todos[idx]
            "Việc (${idx + 1}/${config.todos.size}) • ${todo.time}:${todo.title}"
        } else {
            lunarText
        }

        val clockFormat = if (config.showClockSeconds) "HH:mm:ss" else "HH:mm"
        views.setCharSequence(R.id.clock_main, "setFormat24Hour", clockFormat)
        views.setCharSequence(R.id.clock_main, "setFormat12Hour", clockFormat)

        views.setTextViewText(R.id.tv_solar_date, solarText)
        views.setTextViewText(R.id.tv_lunar_or_todo, line2Text)
        views.setTextViewTextSize(
            R.id.tv_lunar_or_todo,
            android.util.TypedValue.COMPLEX_UNIT_SP,
            config.lunarFontSizeSp
        )

        val iconText = if (config.showWeatherEffect) "${weatherEmoji(config.weatherDescDisplay)} " else ""
        views.setTextViewText(R.id.tv_temp, "$iconText${config.tempDisplay}")
        views.setImageViewResource(R.id.iv_weather_icon, weatherIconRes(config.weatherDescDisplay))
        views.setViewVisibility(
            R.id.iv_weather_icon,
            if (config.showWeatherEffect) android.view.View.VISIBLE else android.view.View.GONE
        )
        views.setTextViewText(R.id.tv_location, config.locationName.ifBlank { "Nghị Đức" })
        views.setTextViewText(R.id.tv_weather_desc, if (config.showWeatherEffect) config.weatherDescDisplay else "")

        if (config.signature.isNotBlank()) {
            views.setViewVisibility(R.id.tv_signature, android.view.View.VISIBLE)
            views.setTextViewText(R.id.tv_signature, config.signature)
        } else {
            views.setViewVisibility(R.id.tv_signature, android.view.View.GONE)
        }

        val isLightFrost = config.widgetStyle == "FROSTED_LIGHT"
        val primaryTextColor = if (isLightFrost) Color.parseColor("#111827") else Color.WHITE
        val secondaryTextColor = if (isLightFrost) Color.parseColor("#374151") else Color.parseColor("#DCE2EC")
        val sigTextColor = if (isLightFrost) Color.parseColor("#4B5563") else Color.parseColor("#B8C2D4")

        when (config.widgetStyle) {
            "TRANSPARENT" -> {
                views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.bg_widget_transparent)
            }
            "FROSTED_LIGHT" -> {
                views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.bg_widget_frosted_light)
            }
            "THEME_COLOR" -> {
                views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.bg_widget_theme)
                views.setInt(R.id.widget_root, "setColorFilter", colorHexForTheme(config.colorThemeKey))
            }
            else -> {
                views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.bg_widget_liquid_dark)
            }
        }

        views.setTextColor(R.id.clock_main, primaryTextColor)
        views.setTextColor(R.id.tv_solar_date, primaryTextColor)
        views.setTextColor(R.id.tv_lunar_or_todo, secondaryTextColor)
        views.setTextColor(R.id.tv_temp, primaryTextColor)
        views.setTextColor(R.id.tv_location, primaryTextColor)
        views.setTextColor(R.id.tv_weather_desc, secondaryTextColor)
        views.setTextColor(R.id.tv_signature, sigTextColor)

        val openIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            if (is4x4) 44 else 42,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        return views
    }
}
