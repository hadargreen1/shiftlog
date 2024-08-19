package com.example.shiftlog

import BaseActivity
import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class SubmitShiftActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var selectedDateTextView: TextView
    private lateinit var previousDayButton: Button
    private lateinit var nextDayButton: Button

    private lateinit var startShiftButton: ImageButton
    private lateinit var startTimeInput: EditText
    private lateinit var endTimeInput: EditText
    private lateinit var submitShiftButton: Button
    private lateinit var timerTextView: TextView

    private var currentDate: Calendar = Calendar.getInstance()
    private var isShiftStarted: Boolean = false
    private var startTime: Long = 0
    private var endTime: Long = 0
    private var totalElapsedTime: Long = 0  // Store total elapsed time
    private var isShiftPaused: Boolean = false  // Track if the shift is paused

    private val handler = Handler(Looper.getMainLooper())
    private var secondsElapsed = 0
    private var isTimerRunning = false
    private var hourlyWage: Double = 0.0  // This will store the user's hourly wage

    private val sharedPrefs by lazy { getSharedPreferences("ShiftPrefs", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_submit_shift)

        setupToolbarAndDrawer(R.id.toolbar, R.id.drawer_layout, R.id.nav_view)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

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

        // Fetch the user's hourly wage from Firestore
        fetchHourlyWage()

        // Clear any previous input when the activity is launched
        clearInputFields()

        // Restore timer state
        restoreTimerState()

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
                endTimeInput.text.clear()  // Clear end time when the shift starts
                isShiftStarted = true
                startTimer()
                saveTimerState()
                Toast.makeText(this, "Shift started. Press again to stop.", Toast.LENGTH_SHORT).show()
            } else {
                // Stop the shift and timer
                endTime = System.currentTimeMillis()
                endTimeInput.setText(getFormattedTime(endTime))
                isShiftStarted = false
                stopTimer()
                saveTimerState()
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
            clearTimerState() // Clear the timer state after saving the shift
        }
    }

    private fun fetchHourlyWage() {
        val user = auth.currentUser?.uid
        if (user != null) {
            firestore.collection("users").document(user).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        hourlyWage = document.getDouble("hourlyWage") ?: 0.0
                    } else {
                        Toast.makeText(this, "No user data found", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error fetching data: ${e.message}", Toast.LENGTH_LONG).show()
                }
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

    @SuppressLint("SetTextI18n")
    private fun saveShiftDataToRealtimeDatabase(date: String, startTime: String, endTime: String) {
        val user = auth.currentUser?.uid
        val db = FirebaseDatabase.getInstance("https://shiftlog-6a430-default-rtdb.europe-west1.firebasedatabase.app").reference

        if (user != null) {
            val duration = calculateDuration(startTime, endTime)
            val salary = calculateSalary(duration, hourlyWage)

            val shiftData = hashMapOf(
                "startTime" to startTime,
                "endTime" to endTime,
                "duration" to duration,
                "salary" to salary
            )

            // Save the shift data under users/{user_id}/shifts/{date}
            db.child("users").child(user).child("shifts").child(date).push().setValue(shiftData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Shift data saved successfully.", Toast.LENGTH_SHORT).show()
                    clearInputFields()
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

    private fun calculateSalary(durationHours: Double, hourlyWage: Double): Double {
        return durationHours * hourlyWage
    }

    // Save the current state of the timer and shift
    private fun saveTimerState() {
        val editor = sharedPrefs.edit()
        editor.putBoolean("isShiftStarted", isShiftStarted)
        editor.putLong("startTime", startTime)
        editor.putLong("endTime", endTime)
        editor.putInt("secondsElapsed", secondsElapsed)
        editor.putBoolean("isTimerRunning", isTimerRunning)
        editor.apply()
    }

    // Restore the saved state of the timer and shift
    private fun restoreTimerState() {
        isShiftStarted = sharedPrefs.getBoolean("isShiftStarted", false)
        startTime = sharedPrefs.getLong("startTime", 0L)
        endTime = sharedPrefs.getLong("endTime", 0L)
        secondsElapsed = sharedPrefs.getInt("secondsElapsed", 0)
        isTimerRunning = sharedPrefs.getBoolean("isTimerRunning", false)

        if (isTimerRunning) {
            startTimer()
        }

        if (isShiftStarted) {
            startTimeInput.setText(getFormattedTime(startTime))
        }

        if (endTime != 0L && !isShiftStarted) {  // Only set end time if the shift is not ongoing
            endTimeInput.setText(getFormattedTime(endTime))
        } else {
            endTimeInput.text.clear()
        }
    }

    // Clear the saved state of the timer and shift
    private fun clearTimerState() {
        val editor = sharedPrefs.edit()
        editor.clear()
        editor.apply()
    }

    // Clear the input fields for start and end times
    private fun clearInputFields() {
        startTimeInput.text.clear()
        endTimeInput.text.clear()
    }
}
