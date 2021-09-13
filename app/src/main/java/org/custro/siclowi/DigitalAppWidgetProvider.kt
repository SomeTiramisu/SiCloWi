/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.custro.siclowi

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.*
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.provider.AlarmClock.ACTION_SHOW_ALARMS
import android.text.format.DateFormat
import android.util.TypedValue.COMPLEX_UNIT_PX
import android.view.LayoutInflater
import android.view.View
import android.view.View.MeasureSpec.UNSPECIFIED
import android.widget.RemoteViews
import android.widget.TextClock
import java.util.*
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * This provider produces a widget resembling one of the formats below.
 *
 * <pre>
 *      12:59 AM
 *      WED, FEB 3
 * </pre>
 *
 * This widget is scaling the font sizes to fit within the widget bounds chosen by the user without
 * any clipping. To do so it measures layouts offscreen using a range of font sizes in order to
 * choose optimal values.
 */
class DigitalAppWidgetProvider : AppWidgetProvider() {
    /**
     * Called when widgets must provide remote views.
     */
    override fun onUpdate(context: Context, wm: AppWidgetManager, widgetIds: IntArray) {
        super.onUpdate(context, wm, widgetIds)

        widgetIds.forEach { widgetId ->
            relayoutWidget(context, wm, widgetId, wm.getAppWidgetOptions(widgetId))
        }
    }

    /**
     * Called when the app widget changes sizes.
     */
    override fun onAppWidgetOptionsChanged(
        context: Context,
        wm: AppWidgetManager?,
        widgetId: Int,
        options: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, wm, widgetId, options)

        // Scale the fonts of the clock to fit inside the new size
        relayoutWidget(context, getInstance(context), widgetId, options)
    }


    /**
     * This class stores the target size of the widget as well as the measured size using a given
     * clock font size. All other fonts and icons are scaled proportional to the clock font.
     */
    private class Sizes(
        val mTargetWidthPx: Int,
        val mTargetHeightPx: Int,
        val largestClockFontSizePx: Int
    ) {
        val smallestClockFontSizePx = 1

        var mMeasuredWidthPx = 0
        var mMeasuredHeightPx = 0
        var mMeasuredTextClockWidthPx = 0
        var mMeasuredTextClockHeightPx = 0

        /** The size of the font to use on the date / next alarm time fields.  */
        var mFontSizePx = 0

        /** The size of the font to use on the clock field.  */
        var mClockFontSizePx = 0

        var mIconFontSizePx = 0
        var mIconPaddingPx = 0

        var clockFontSizePx: Int
            get() = mClockFontSizePx
            set(clockFontSizePx) {
                mClockFontSizePx = clockFontSizePx
                mFontSizePx = max(1, (clockFontSizePx / 7.5f).roundToInt())
                mIconFontSizePx = (mFontSizePx * 1.4f).toInt()
                mIconPaddingPx = mFontSizePx / 3
            }

        fun hasViolations(): Boolean {
            return mMeasuredWidthPx > mTargetWidthPx || mMeasuredHeightPx > mTargetHeightPx
        }

        fun newSize(): Sizes {
            return Sizes(mTargetWidthPx, mTargetHeightPx, largestClockFontSizePx)
        }

        override fun toString(): String {
            val builder = StringBuilder(1000)
            builder.append("\n")
            append(builder, "Target dimensions: %dpx x %dpx\n", mTargetWidthPx, mTargetHeightPx)
            append(builder, "Last valid widget container measurement: %dpx x %dpx\n",
                    mMeasuredWidthPx, mMeasuredHeightPx)
            append(builder, "Last text clock measurement: %dpx x %dpx\n",
                    mMeasuredTextClockWidthPx, mMeasuredTextClockHeightPx)
            if (mMeasuredWidthPx > mTargetWidthPx) {
                append(builder, "Measured width %dpx exceeded widget width %dpx\n",
                        mMeasuredWidthPx, mTargetWidthPx)
            }
            if (mMeasuredHeightPx > mTargetHeightPx) {
                append(builder, "Measured height %dpx exceeded widget height %dpx\n",
                        mMeasuredHeightPx, mTargetHeightPx)
            }
            append(builder, "Clock font: %dpx\n", mClockFontSizePx)
            return builder.toString()
        }

        companion object {
            private fun append(builder: StringBuilder, format: String, vararg args: Any) {
                builder.append(String.format(Locale.ENGLISH, format, *args))
            }
        }
    }

    companion object {
        private val LOGGER = LogUtils.Logger("DigitalWidgetProvider")

        /**
         * Compute optimal font and icon sizes offscreen for both portrait and landscape orientations
         * using the last known widget size and apply them to the widget.
         */
        private fun relayoutWidget(
            context: Context,
            wm: AppWidgetManager,
            widgetId: Int,
            options: Bundle
        ) {
            val portrait: RemoteViews = relayoutWidget(context, wm, widgetId, options, true)
            val landscape: RemoteViews = relayoutWidget(context, wm, widgetId, options, false)
            val widget = RemoteViews(landscape, portrait)
            wm.updateAppWidget(widgetId, widget)
        }

        /**
         * Compute optimal font and icon sizes offscreen for the given orientation.
         */
        private fun relayoutWidget(
            context: Context,
            wm: AppWidgetManager,
            widgetId: Int,
            options: Bundle?,
            portrait: Boolean
        ): RemoteViews {
            // Create a remote view for the digital clock.
            val packageName: String = context.packageName
            val rv = RemoteViews(packageName, R.layout.digital_widget)

            // Tapping on the widget opens the app (if not on the lock screen).
            if (WidgetUtils.isWidgetClickable(wm, widgetId)) {
                val openApp = Intent(ACTION_SHOW_ALARMS)
                val pi: PendingIntent = PendingIntent.getActivity(context, 0, openApp, 0)
                rv.setOnClickPendingIntent(R.id.digital_widget, pi)
            }

            // Configure child views of the remote view.
            val dateFormat: CharSequence = getDateFormat(context)
            rv.setCharSequence(R.id.date, "setFormat12Hour", dateFormat)
            rv.setCharSequence(R.id.date, "setFormat24Hour", dateFormat)

            val options = options ?: wm.getAppWidgetOptions(widgetId)

            // Fetch the widget size selected by the user.
            val resources: Resources = context.resources
            val density: Float = resources.displayMetrics.density
            val minWidthPx = (density * options.getInt(OPTION_APPWIDGET_MIN_WIDTH)).toInt()
            val minHeightPx = (density * options.getInt(OPTION_APPWIDGET_MIN_HEIGHT)).toInt()
            val maxWidthPx = (density * options.getInt(OPTION_APPWIDGET_MAX_WIDTH)).toInt()
            val maxHeightPx = (density * options.getInt(OPTION_APPWIDGET_MAX_HEIGHT)).toInt()
            val targetWidthPx = if (portrait) minWidthPx else maxWidthPx
            val targetHeightPx = if (portrait) maxHeightPx else minHeightPx
            val largestClockFontSizePx: Int =
                    resources.getDimensionPixelSize(R.dimen.widget_max_clock_font_size)

            // Create a size template that describes the widget bounds.
            val template = Sizes(targetWidthPx, targetHeightPx, largestClockFontSizePx)

            // Compute optimal font sizes and icon sizes to fit within the widget bounds.
            val sizes = optimizeSizes(context, template)
            if (LOGGER.isVerboseLoggable) {
                LOGGER.v(sizes.toString())
            }

            // Apply the computed sizes to the remote views.
            rv.setTextViewTextSize(R.id.date, COMPLEX_UNIT_PX, sizes.mFontSizePx.toFloat())
            rv.setTextViewTextSize(R.id.clock, COMPLEX_UNIT_PX, sizes.mClockFontSizePx.toFloat())

            return rv
        }

        /**
         * Inflate an offscreen copy of the widget views. Binary search through the range of sizes
         * until the optimal sizes that fit within the widget bounds are located.
         */
        private fun optimizeSizes(
            context: Context,
            template: Sizes
        ): Sizes {
            // Inflate a test layout to compute sizes at different font sizes.
            val inflater: LayoutInflater = LayoutInflater.from(context)
            @SuppressLint("InflateParams") val sizer: View =
                    inflater.inflate(R.layout.digital_widget_sizer, null /* root */)

            // Configure the date to display the current date string.
            val dateFormat: CharSequence = getDateFormat(context)
            val date: TextClock = sizer.findViewById(R.id.date) as TextClock
            date.format12Hour = dateFormat
            date.format24Hour = dateFormat

            // Measure the widget at the largest possible size.
            var high = measure(template, template.largestClockFontSizePx, sizer)
            if (!high.hasViolations()) {
                return high
            }

            // Measure the widget at the smallest possible size.
            var low = measure(template, template.smallestClockFontSizePx, sizer)
            if (low.hasViolations()) {
                return low
            }

            // Binary search between the smallest and largest sizes until an optimum size is found.
            while (low.clockFontSizePx != high.clockFontSizePx) {
                val midFontSize: Int = (low.clockFontSizePx + high.clockFontSizePx) / 2
                if (midFontSize == low.clockFontSizePx) {
                    return low
                }
                val midSize = measure(template, midFontSize, sizer)
                if (midSize.hasViolations()) {
                    high = midSize
                } else {
                    low = midSize
                }
            }

            return low
        }

        /**
         * Compute all font and icon sizes based on the given `clockFontSize` and apply them to
         * the offscreen `sizer` view. Measure the `sizer` view and return the resulting
         * size measurements.
         */
        private fun measure(template: Sizes, clockFontSize: Int, sizer: View): Sizes {
            // Create a copy of the given template sizes.
            val measuredSizes = template.newSize()

            // Configure the clock to display the widest time string.
            val date: TextClock = sizer.findViewById(R.id.date) as TextClock
            val clock: TextClock = sizer.findViewById(R.id.clock) as TextClock

            // Adjust the font sizes.
            measuredSizes.clockFontSizePx = clockFontSize
            clock.text = getLongestTimeString(clock)
            clock.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mClockFontSizePx.toFloat())
            date.setTextSize(COMPLEX_UNIT_PX, measuredSizes.mFontSizePx.toFloat())

            // Measure and layout the sizer.
            val widthSize: Int = View.MeasureSpec.getSize(measuredSizes.mTargetWidthPx)
            val heightSize: Int = View.MeasureSpec.getSize(measuredSizes.mTargetHeightPx)
            val widthMeasureSpec: Int = View.MeasureSpec.makeMeasureSpec(widthSize, UNSPECIFIED)
            val heightMeasureSpec: Int = View.MeasureSpec.makeMeasureSpec(heightSize, UNSPECIFIED)
            sizer.measure(widthMeasureSpec, heightMeasureSpec)
            sizer.layout(0, 0, sizer.measuredWidth, sizer.measuredHeight)

            // Copy the measurements into the result object.
            measuredSizes.mMeasuredWidthPx = sizer.measuredWidth
            measuredSizes.mMeasuredHeightPx = sizer.measuredHeight
            measuredSizes.mMeasuredTextClockWidthPx = clock.measuredWidth
            measuredSizes.mMeasuredTextClockHeightPx = clock.measuredHeight

            return measuredSizes
        }

        /**
         * @return "11:59" or "23:59" in the current locale
         */
        private fun getLongestTimeString(clock: TextClock): CharSequence {
            val format: CharSequence = if (clock.is24HourModeEnabled) {
                clock.format24Hour
            } else {
                clock.format12Hour
            }
            val longestPMTime = Calendar.getInstance()
            longestPMTime[0, 0, 0, 23] = 59
            return DateFormat.format(format, longestPMTime)
        }

        /**
         * @return the locale-specific date pattern
         */
        private fun getDateFormat(context: Context): String {
            val locale = Locale.getDefault()
            val skeleton: String = context.getString(R.string.abbrev_wday_month_day_no_year)
            return DateFormat.getBestDateTimePattern(locale, skeleton)
        }
    }
}