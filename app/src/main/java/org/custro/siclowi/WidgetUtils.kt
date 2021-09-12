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
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.View

object WidgetUtils {
    // Calculate the scale factor of the fonts in the widget
    fun getScaleRatio(context: Context, options: Bundle?, id: Int, cityCount: Int): Float {
        var options: Bundle? = options
        if (options == null) {
            val widgetManager: AppWidgetManager =
                    AppWidgetManager.getInstance(context) // no manager , do no scaling
                    ?: return 1f
            options = widgetManager.getAppWidgetOptions(id)
        }
        options?.let {
            val minWidth: Int = it.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            if (minWidth == 0) {
                // No data , do no scaling
                return 1f
            }
            val res: Resources = context.getResources()
            val density: Float = res.getDisplayMetrics().density
            var ratio: Float =
                    density * minWidth / res.getDimension(R.dimen.min_digital_widget_width)
            ratio = Math.min(ratio, getHeightScaleRatio(context, it))
            ratio *= .83f

            if (cityCount > 0) {
                return if (ratio > 1f) 1f else ratio
            }

            ratio = Math.min(ratio, 1.6f)
            ratio = if (isPortrait(context)) {
                Math.max(ratio, .71f)
            } else {
                Math.max(ratio, .45f)
            }
            return ratio
        }
        return 1f
    }

    // Calculate the scale factor of the fonts in the list of the widget using the widget height
    private fun getHeightScaleRatio(context: Context, options: Bundle): Float {
        val minHeight: Int = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        if (minHeight == 0) {
            // No data , do no scaling
            return 1f
        }
        val res: Resources = context.getResources()
        val density: Float = res.getDisplayMetrics().density
        val ratio: Float = density * minHeight / res.getDimension(R.dimen.min_digital_widget_height)
        return if (isPortrait(context)) { ratio * 1.75f } else ratio
    }

    /**
     * @return `true` iff the widget is being hosted in a container where tapping is allowed
     */
    fun isWidgetClickable(widgetManager: AppWidgetManager, widgetId: Int): Boolean {
        val wo = widgetManager.getAppWidgetOptions(widgetId)
        return (wo != null &&
                wo.getInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, -1)
                != AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD)
    }

    /**
     * This method assumes the given `view` has already been layed out.
     *
     * @return a Bitmap containing an image of the `view` at its current size
     */
    fun createBitmap(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    /**
     * @param context from which to query the current device configuration
     * @return `true` if the device is currently in portrait or reverse portrait orientation
     */
    fun isPortrait(context: Context): Boolean {
        return context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    }

}