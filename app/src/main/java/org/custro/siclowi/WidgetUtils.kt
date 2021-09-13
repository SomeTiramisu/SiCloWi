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

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo

object WidgetUtils {

    /**
     * @return `true` iff the widget is being hosted in a container where tapping is allowed
     */
    fun isWidgetClickable(widgetManager: AppWidgetManager, widgetId: Int): Boolean {
        val wo = widgetManager.getAppWidgetOptions(widgetId)
        return (wo != null &&
                wo.getInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, -1)
                != AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD)
    }
}