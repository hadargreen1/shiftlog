import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.view.View
import android.widget.DatePicker
import java.util.*

@SuppressLint("DiscouragedApi")
class MonthYearPickerDialog(
    context: Context,
    private val listener: (year: Int, month: Int) -> Unit
) : DatePickerDialog(context, null, Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH), Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) {

    init {
        this.setOnDateSetListener { _: DatePicker, year: Int, month: Int, _: Int ->
            listener(year, month)
        }

        val dayId = context.resources.getIdentifier("day", "id", "android")
        this.datePicker.findViewById<View>(dayId)?.visibility = View.GONE
    }

    override fun onDateChanged(view: DatePicker, year: Int, month: Int, day: Int) {
        view?.let { super.onDateChanged(it, year, month, day) }
        this.setTitle("Select Month and Year")
    }
}
