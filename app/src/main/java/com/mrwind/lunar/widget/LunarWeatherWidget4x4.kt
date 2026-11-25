package com.mrwind.lunar.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

class LunarWeatherWidget4x4 : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val config = com.mrwind.lunar.data.AppPrefs.loadConfig(context)
        for (appWidgetId in appWidgetIds) {
            val views = WidgetRenderer.buildRemoteViews(context, config, is4x4 = true)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
