package utilities

import android.app.AlertDialog
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.shiftlog.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import java.text.SimpleDateFormat
import java.util.*

class CalendarUtility : Fragment() {

    private lateinit var calendarView: MaterialCalendarView
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_calendar, container, false)
        calendarView = view.findViewById(R.id.calendarView)

        calendarView.setOnDateChangedListener { _, date, _ ->
            val selectedDate = convertCalendarDayToDate(date)
            val formattedDate =
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate)
            loadShiftDataForDate(formattedDate)
        }

        // Load worked days from the database and apply decorators
        loadWorkedDaysFromDatabase()

        return view
    }

    private fun convertCalendarDayToDate(day: CalendarDay): Date {
        val calendar = Calendar.getInstance()
        calendar.set(day.year, day.month - 1, day.day)
        return calendar.time
    }

    private fun loadShiftDataForDate(date: String) {
        val user = auth.currentUser?.uid
        if (user != null) {
            val db =
                FirebaseDatabase.getInstance("https://shiftlog-6a430-default-rtdb.europe-west1.firebasedatabase.app").reference
            val dateRef = db.child("users").child(user).child("shifts").child(date)

            dateRef.get().addOnSuccessListener { dataSnapshot ->
                if (dataSnapshot.exists()) {
                    val shiftList = mutableListOf<Map<String, Any>>()

                    for (shiftSnapshot in dataSnapshot.children) {
                        val shiftData = shiftSnapshot.getValue(object :
                            GenericTypeIndicator<Map<String, Any>>() {})
                        if (shiftData != null) {
                            shiftList.add(shiftData)
                        }
                    }

                    if (shiftList.isNotEmpty()) {
                        displayShiftData(shiftList, date)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "No shifts recorded for this date.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "No shifts recorded for this date.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }.addOnFailureListener {
                Toast.makeText(
                    requireContext(),
                    "Error loading shift data: ${it.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            Toast.makeText(requireContext(), "User not authenticated.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun displayShiftData(shiftArray: List<Map<String, Any>>, date: String) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_shift_details, null)
        val tvShiftDetails = dialogView.findViewById<TextView>(R.id.tvShiftDetails)
        val detailsBuilder = StringBuilder()

        val locale = Locale.getDefault()  // Or specify a particular locale if needed

        shiftArray.forEachIndexed { _, shift ->
            val startTime = shift["startTime"] as? String ?: "N/A"
            val endTime = shift["endTime"] as? String ?: "N/A"

            // Handle possible different types for duration and salary
            val duration = when (val d = shift["duration"]) {
                is Number -> d.toDouble()
                else -> 0.0
            }
            val salary = when (val s = shift["salary"]) {
                is Number -> s.toDouble()
                else -> 0.0
            }

            Log.d("DisplayShiftData", "Shift - Start Time: $startTime, End Time: $endTime, Duration: $duration, Salary: $salary")

            detailsBuilder.append("Shift Date: $date\n")
            detailsBuilder.append("Start Time: $startTime\n")
            detailsBuilder.append("End Time: $endTime\n")
            detailsBuilder.append("Duration: ${String.format(locale, "%.2f", duration)} hrs\n")
            detailsBuilder.append("Salary: $${String.format(locale, "%.2f", salary)}\n\n")
        }

        tvShiftDetails.text = detailsBuilder.toString()

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle("Shift Details")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun loadWorkedDaysFromDatabase() {
        val user = auth.currentUser?.uid
        if (user != null) {
            val db = FirebaseDatabase.getInstance("https://shiftlog-6a430-default-rtdb.europe-west1.firebasedatabase.app").reference
            val userShiftsRef = db.child("users").child(user).child("shifts")

            userShiftsRef.get().addOnSuccessListener { dataSnapshot ->
                val workedDays = HashSet<CalendarDay>()

                for (dateSnapshot in dataSnapshot.children) {
                    val dateStr = dateSnapshot.key
                    dateStr?.let {
                        // Convert the date string (yyyy-MM-dd) to CalendarDay
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val date = sdf.parse(it)
                        date?.let { parsedDate ->
                            val calendar = Calendar.getInstance()
                            calendar.time = parsedDate
                            val workedDay = CalendarDay.from(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))
                            workedDays.add(workedDay)
                        }
                    }
                }

                // Apply the decorator to highlight the worked days
                val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.round_background)
                calendarView.addDecorator(WorkedDaysDecorator(workedDays, drawable))
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Error loading worked days: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Decorator class to highlight the worked days with a round background
    class WorkedDaysDecorator(private val workedDays: HashSet<CalendarDay>, private val drawable: Drawable?) : DayViewDecorator {

        override fun shouldDecorate(day: CalendarDay): Boolean {
            return workedDays.contains(day)
        }

        override fun decorate(view: DayViewFacade) {
            drawable?.let {
                view.setBackgroundDrawable(it) // Set the round background
            }
        }
    }
}
