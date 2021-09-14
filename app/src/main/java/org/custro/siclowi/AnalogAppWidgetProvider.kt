/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.custro.siclowi

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.widget.RemoteViews

/**
 * Simple widget to show an analog clock.
 */
class AnalogAppWidgetProvider : AppWidgetProvider() {
    /**
     * Called when widgets must provide remote views.
     */
    override fun onUpdate(context: Context, wm: AppWidgetManager, widgetIds: IntArray) {
        super.onUpdate(context, wm, widgetIds)
        widgetIds.forEach { widgetId ->
            val packageName: String = context.packageName
            val widget = RemoteViews(packageName, R.layout.analog_appwidget)

            // Tapping on the widget opens the app (if not on the lock screen).
            if (WidgetUtils.isWidgetClickable(wm, widgetId)) {
                //val openApp = Intent(AlarmClock.ACTION_SHOW_ALARMS)
                val openApp = context.packageManager.getLaunchIntentForPackage("com.android.deskclock")
                val pi: PendingIntent = PendingIntent.getActivity(context, 0, openApp, 0)
                widget.setOnClickPendingIntent(R.id.analog_appwidget, pi)
            }
            wm.updateAppWidget(widgetId, widget)
        }
    }
}