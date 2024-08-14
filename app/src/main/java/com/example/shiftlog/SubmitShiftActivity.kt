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
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
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
    private var isShiftStopped: Boolean = false  // Track if the shift has stopped
    private var isTimerRunning: Boolean = false  // Track if the timer is running
    private var startTime: Long = 0
    private var totalDuration: Long = 0
    private var handler = Handler(Looper.getMainLooper())
    private var secondsElapsed: Int = 0

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
                // Start the shift
                startTime = System.currentTimeMillis()
                startTimeInput.setText(getFormattedTime(startTime))
                isShiftStarted = true
                isTimerRunning = true
                startTimer()
                Toast.makeText(this, "Shift started. Press again to stop.", Toast.LENGTH_SHORT).show()
            } else if (!isShiftStopped) {
                // Stop the shift
                val endTime = System.currentTimeMillis()
                endTimeInput.setText(getFormattedTime(endTime))
                totalDuration += endTime - startTime
                isShiftStopped = true
                isTimerRunning = false
                stopTimer()
                Toast.makeText(this, "Shift ended. Please submit the shift.", Toast.LENGTH_SHORT).show()
            }
        }

        // Handle shift submission
        submitShiftButton.setOnClickListener {
            if (isShiftStarted && isShiftStopped) {
                val startTimeText = startTimeInput.text.toString()
                val endTimeText = endTimeInput.text.toString()

                if (startTimeText.isEmpty() || endTimeText.isEmpty()) {
                    Toast.makeText(this, "Please enter both start and end times.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val selectedDateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentDate.time)

                saveShiftData(selectedDateString, startTimeText, endTimeText)

                // Reset the state for the next shift
                isShiftStarted = false
                isShiftStopped = false
                totalDuration = 0
                secondsElapsed = 0
                startTimeInput.text.clear()
                endTimeInput.text.clear()
                timerTextView.text = "00:00:00"
                Toast.makeText(this, "Shift submitted successfully. You can start a new shift.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please complete the shift before submitting.", Toast.LENGTH_SHORT).show()
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
        handler.post(object : Runnable {
            override fun run() {
                if (isTimerRunning) {
                    secondsElapsed++
                    val hours = secondsElapsed / 3600
                    val minutes = (secondsElapsed % 3600) / 60
                    val seconds = secondsElapsed % 60
                    val time = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                    timerTextView.text = time
                    handler.postDelayed(this, 1000)
                }
            }
        })
    }

    private fun stopTimer() {
        isTimerRunning = false
    }

    private fun saveShiftData(date: String, startTime: String, endTime: String) {
        val user = auth.currentUser?.uid
        val db = Firebase.firestore

        if (user != null) {
            val userDocRef = db.collection("users").document(user).collection("shifts").document(date)

            userDocRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    updateTimes(document, startTime, endTime, userDocRef)
                } else {
                    initDocument(userDocRef, startTime, endTime)
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Error getting document: ${it.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "User is not authenticated.", Toast.LENGTH_LONG).show()
        }
    }

    private fun initDocument(userDocRef: DocumentReference, startTime: String, endTime: String) {
        val shiftData = hashMapOf(
            "startTime" to startTime,
            "endTime" to endTime,
            "duration" to calculateDuration(startTime, endTime),
            "salary" to calculateSalary(calculateDuration(startTime, endTime))
        )
        val shiftArray = arrayListOf(shiftData)
        userDocRef.set(mapOf("shiftArray" to shiftArray))
            .addOnSuccessListener {
                Toast.makeText(this, "Shift data initialized successfully.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error initializing shift data: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun updateTimes(document: DocumentSnapshot, startTime: String, endTime: String, userDocRef: DocumentReference) {
        val newShiftData = hashMapOf(
            "startTime" to startTime,
            "endTime" to endTime,
            "duration" to calculateDuration(startTime, endTime),
            "salary" to calculateSalary(calculateDuration(startTime, endTime))
        )
        val shiftArray = (document.get("shiftArray") as? List<*>)?.mapNotNull { it as? Map<String, Any> } ?: emptyList()
        val shiftArrayList = ArrayList(shiftArray)
        shiftArrayList.add(newShiftData)

        userDocRef.update("shiftArray", shiftArrayList)
            .addOnSuccessListener {
                Toast.makeText(this, "Shift data updated successfully.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error updating shift data: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun calculateDuration(startTime: String, endTime: String): Double {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val start = sdf.parse(startTime)?.time ?: 0L
        val end = sdf.parse(endTime)?.time ?: 0L

        return (end - start) / (1000.0 * 60 * 60) + totalDuration / (1000.0 * 60 * 60) // Convert milliseconds to hours and add total duration
    }

    private fun calculateSalary(durationHours: Double): Double {
        val hourlyRate = 20.0  // Example hourly rate
        return durationHours * hourlyRate
    }
}
