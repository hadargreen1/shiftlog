import android.graphics.Color
import android.text.style.ForegroundColorSpan
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.CalendarDay

class WorkedDaysDecorator(private val workedDays: HashSet<CalendarDay>, private val color: Int) : DayViewDecorator {

    override fun shouldDecorate(day: CalendarDay): Boolean {
        // Decorate only the days that the user worked on
        return workedDays.contains(day)
    }

    override fun decorate(view: DayViewFacade) {
        // Change the background color of the day or apply a visual indicator
        view.addSpan(ForegroundColorSpan(color)) // Change text color
        // view.setBackgroundDrawable(yourCustomDrawable) // Optionally set a background drawable
    }
}
