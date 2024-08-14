package com.example.shiftlog

import android.content.Context
import android.graphics.Color
import android.text.style.ForegroundColorSpan
import androidx.core.content.ContextCompat
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.CalendarDay

// Custom DayViewDecorator class to highlight worked days
class WorkedDayDecorator(private val context: Context, private val workedDays: HashSet<CalendarDay>) : DayViewDecorator {
    override fun shouldDecorate(day: CalendarDay): Boolean {
        return workedDays.contains(day)
    }

    override fun decorate(view: DayViewFacade) {
        // This will add a circle around the day and change the text color
        val drawable = ContextCompat.getDrawable(context, R.drawable.circle_background)
        if (drawable != null) {
            view.setBackgroundDrawable(drawable)
        }
        view.addSpan(ForegroundColorSpan(Color.WHITE)) // Example to change text color
    }
}
