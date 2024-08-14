package com.example.shiftlog

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class SubmitShiftActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var selectedDateTextView: TextView
    private lateinit var previousDayButton: Button
    private lateinit var nextDayButton: Button

    private lateinit var startShiftButton: FloatingActionButton
    private lateinit var startTimeInput: EditText
    private lateinit var endTimeInput: EditText
    private lateinit var submitShiftButton: Button
    private lateinit var timerTextView: TextView

    private var currentDate: Calendar = Calendar.getInstance()
    private var isShiftStarted: Boolean = false  // Track if the shift has started
    private var startTime: Long = 0
    private var endTime: Long = 0

    private val handler = Handler(Looper.getMainLooper())
    private var secondsElapsed = 0
    private var isTimerRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_submit_shift)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Link UI elements
        selectedDateTextView = findViewById(R.id.selectedDateTextView)
        previousDayButton = findViewById(R.id.previousDayButton)
        nextDayButton = findViewById(R.id.nextDayButton)
        startShiftButton = findViewById(R.id.startShiftButton)
        startTimeInput = findViewById(R.id.startTimeInput)
        endTimeInput = findViewById(R.id.endTimeInput)
        submitShiftButton = findViewById(R.id.submitShiftButton)
        timerTextView = findViewById(R.id.timerTextView)

        // Set initial date to today
        updateDisplayedDate()

        // Handle previous day button click
        previousDayButton.setOnClickListener {
            currentDate.add(Calendar.DAY_OF_MONTH, -1)
            updateDisplayedDate()
        }

        // Handle next day button click
        nextDayButton.setOnClickListener {
            currentDate.add(Calendar.DAY_OF_MONTH, 1)
            updateDisplayedDate()
        }

        // Handle start and stop shift
        startShiftButton.setOnClickListener {
            if (!isShiftStarted) {
                // Start the shift and timer
                startTime = System.currentTimeMillis()
                startTimeInput.setText(getFormattedTime(startTime))
                isShiftStarted = true
                startTimer()
                Toast.makeText(this, "Shift started. Press again to stop.", Toast.LENGTH_SHORT).show()
            } else {
                // Stop the shift and timer
                endTime = System.currentTimeMillis()
                endTimeInput.setText(getFormattedTime(endTime))
                isShiftStarted = false
                stopTimer()
                Toast.makeText(this, "Shift ended. Now, press Submit to save.", Toast.LENGTH_SHORT).show()
            }
        }

        // Handle shift submission
        submitShiftButton.setOnClickListener {
            val startTimeText = startTimeInput.text.toString()
            val endTimeText = endTimeInput.text.toString()

            if (startTimeText.isEmpty() || endTimeText.isEmpty()) {
                Toast.makeText(this, "Please enter both start and end times.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedDateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentDate.time)
            saveShiftDataToRealtimeDatabase(selectedDateString, startTimeText, endTimeText)
        }
    }

    private fun updateDisplayedDate() {
        val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentDate.time)
        selectedDateTextView.text = formattedDate
    }

    private fun getFormattedTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun startTimer() {
        isTimerRunning = true
        secondsElapsed = 0
        handler.post(object : Runnable {
            override fun run() {
                if (isTimerRunning) {
                    val hours = secondsElapsed / 3600
                    val minutes = (secondsElapsed % 3600) / 60
                    val seconds = secondsElapsed % 60
                    timerTextView.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                    secondsElapsed++
                    handler.postDelayed(this, 1000)
                }
            }
        })
    }

    private fun stopTimer() {
        isTimerRunning = false
        handler.removeCallbacksAndMessages(null)
    }
    private fun saveShiftDataToRealtimeDatabase(date: String, startTime: String, endTime: String) {
        val user = auth.currentUser?.uid
        val db = FirebaseDatabase.getInstance("https://shiftlog-6a430-default-rtdb.europe-west1.firebasedatabase.app").reference

        if (user != null) {
            val shiftData = hashMapOf(
                "startTime" to startTime,
                "endTime" to endTime,
                "duration" to calculateDuration(startTime, endTime),
                "salary" to calculateSalary(calculateDuration(startTime, endTime))
            )

            // Save the shift data under users/{user_id}/shifts/{date}
            db.child("users").child(user).child("shifts").child(date).push().setValue(shiftData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Shift data saved successfully.", Toast.LENGTH_SHORT).show()
                    // Clear input fields and reset the timer
                    startTimeInput.text.clear()
                    endTimeInput.text.clear()
                    timerTextView.text = "00:00:00"
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error saving shift data: ${it.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            Toast.makeText(this, "User is not authenticated.", Toast.LENGTH_LONG).show()
        }
    }



    private fun calculateDuration(startTime: String, endTime: String): Double {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val start = sdf.parse(startTime)?.time ?: 0L
        val end = sdf.parse(endTime)?.time ?: 0L
        return (end - start) / (1000.0 * 60 * 60)  // Convert milliseconds to hours
    }

    private fun calculateSalary(durationHours: Double): Double {
        val hourlyRate = 20.0  // Example hourly rate
        return durationHours * hourlyRate
    }

}