package utilities

import android.graphics.drawable.Drawable
import android.text.style.ForegroundColorSpan
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade

class EventDecorator(
    private val color: Int,
    private val dates: HashSet<CalendarDay>,
    private val drawable: Drawable
) : DayViewDecorator {

    override fun shouldDecorate(day: CalendarDay): Boolean {
        // Decorate only the dates that are in the set
        return dates.contains(day)
    }

    override fun decorate(view: DayViewFacade) {
        // Set the background drawable for the selected dates
        view.setBackgroundDrawable(drawable)
        // Optionally, you can also change the text color
        view.addSpan(ForegroundColorSpan(color))
    }
}
