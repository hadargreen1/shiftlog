package com.example.shiftlog

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import com.prolificinteractive.materialcalendarview.CalendarDay
import java.text.SimpleDateFormat
import java.util.*
import android.widget.TextView
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import kotlin.text.*

class CalendarFragment : Fragment() {

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


}