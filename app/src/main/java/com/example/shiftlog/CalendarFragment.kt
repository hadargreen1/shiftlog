package com.example.shiftlog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import com.prolificinteractive.materialcalendarview.CalendarDay
import java.text.SimpleDateFormat
import java.util.*
import android.widget.TextView

class CalendarFragment : Fragment() {

    private lateinit var calendarView: MaterialCalendarView
    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_calendar, container, false)
        calendarView = view.findViewById(R.id.calendarView)

        calendarView.setOnDateChangedListener { _, date, _ ->
            // Convert CalendarDay to Date
            val selectedDate = convertCalendarDayToDate(date)
            val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate)
            loadShiftDataForDate(formattedDate)
        }

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
            val dateDocRef = db.collection("users").document(user).collection("shifts").document(date)

            dateDocRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val shiftArray = document.get("shiftArray") as? List<*>
                    if (shiftArray != null) {
                        displayShiftData(shiftArray)
                    } else {
                        Toast.makeText(requireContext(), "No shifts recorded for this date.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "No shifts recorded for this date.", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Error loading shift data: ${it.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(requireContext(), "User not authenticated.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun displayShiftData(shiftArray: List<*>) {
        // Inflate the custom layout
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_shift_details, null)

        // Find the TextViews in the custom layout
        val tvShiftStartTime = dialogView.findViewById<TextView>(R.id.tvShiftStartTime)
        val tvShiftEndTime = dialogView.findViewById<TextView>(R.id.tvShiftEndTime)
        val tvShiftDuration = dialogView.findViewById<TextView>(R.id.tvShiftDuration)
        val tvShiftSalary = dialogView.findViewById<TextView>(R.id.tvShiftSalary)

        // Assuming you want to show only the first shift for simplicity
        val firstShift = shiftArray.firstOrNull() as? Map<*, *>
        firstShift?.let {
            val startTime = it["startTime"] as? String ?: "N/A"
            val endTime = it["endTime"] as? String ?: "N/A"
            val duration = it["duration"] as? Double ?: 0.0
            val salary = it["salary"] as? Double ?: 0.0

            // Populate the TextViews with the shift data
            tvShiftStartTime.text = "Shift Start Time: $startTime"
            tvShiftEndTime.text = "Shift End Time: $endTime"
            tvShiftDuration.text = "Total Hours Worked: ${"%.2f".format(duration)} hrs"
            tvShiftSalary.text = "Salary Earned: $${"%.2f".format(salary)}"
        }

        // Create and show the dialog
        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle("Shift Details")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}
