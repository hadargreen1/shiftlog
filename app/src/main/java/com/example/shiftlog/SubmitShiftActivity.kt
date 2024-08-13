package com.example.shiftlog

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

class SubmitShiftActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var startShiftButton: FloatingActionButton
    private lateinit var startTimeInput: EditText
    private lateinit var endTimeInput: EditText
    private lateinit var submitShiftButton: Button
    private var startTime: Long = 0  // Store the start time as a timestamp
    private var endTime: Long = 0  // Store the end time as a timestamp
    private var isShiftStarted: Boolean = false  // Flag to track shift state

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_submit_shift)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Link UI elements
        startShiftButton = findViewById(R.id.startShiftButton)
        startTimeInput = findViewById(R.id.startTimeInput)
        endTimeInput = findViewById(R.id.endTimeInput)
        submitShiftButton = findViewById(R.id.submitShiftButton)

        startShiftButton.setOnClickListener {
            if (!isShiftStarted) {
                // Start the shift and record the start time
                startTime = System.currentTimeMillis()
                startTimeInput.setText(getFormattedTime(startTime))
                isShiftStarted = true
                Toast.makeText(this, "Shift started. Press again to stop.", Toast.LENGTH_SHORT).show()
            } else {
                // End the shift and record the end time
                endTime = System.currentTimeMillis()
                endTimeInput.setText(getFormattedTime(endTime))
                isShiftStarted = false  // Reset the flag for the next shift
                Toast.makeText(this, "Shift ended. Now, press Submit to save.", Toast.LENGTH_SHORT).show()
            }
        }

        submitShiftButton.setOnClickListener {
            // Get the input values
            val startTimeText = startTimeInput.text.toString()
            val endTimeText = endTimeInput.text.toString()

            // Validate inputs
            if (startTimeText.isEmpty() || endTimeText.isEmpty()) {
                Toast.makeText(this, "Please enter both start and end times.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Convert times to timestamps if manually entered
            startTime = getTimestampFromTime(startTimeText)
            endTime = getTimestampFromTime(endTimeText)

            // Calculate duration and salary
            val durationHours = calculateDuration(startTime, endTime)
            val salary = calculateSalary(durationHours)

            // Save everything to Firebase
            saveShiftData(startTimeText, endTimeText, durationHours, salary)
        }
    }

    private fun getFormattedTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun getTimestampFromTime(time: String): Long {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.parse(time)?.time ?: 0L
    }

    private fun calculateDuration(startTime: Long, endTime: Long): Double {
        val durationMillis = endTime - startTime
        return durationMillis / (1000.0 * 60 * 60)  // Convert milliseconds to hours
    }

    private fun calculateSalary(durationHours: Double): Double {
        val hourlyRate = 20.0  // Example hourly rate
        return durationHours * hourlyRate
    }

    private fun saveShiftData(startTime: String, endTime: String, duration: Double, salary: Double) {
        val user = auth.currentUser?.uid
        val db = Firebase.firestore

        if (user != null) {
            // Reference to Firebase Realtime Database
            val database = FirebaseDatabase.getInstance()
            val shiftsRef = database.getReference("shifts").child(user)

            // Create a map for the shift data
            val shiftData = hashMapOf(
                "startTime" to startTime,
                "endTime" to endTime,
                "durationHours" to duration.toString(),
                "salary" to salary.toString()
            )

            // Save the shift data in Firestore
            db.collection("users").document(user).collection("shifts").add(shiftData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Shift data saved successfully.", Toast.LENGTH_SHORT).show()
                    // Save the same data to Realtime Database as well
                    shiftsRef.push().setValue(shiftData).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Shift data saved successfully.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(
                                this,
                                "Failed to save shift data: ${task.exception?.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to save shift data: ${it.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            Toast.makeText(this, "User is not authenticated.", Toast.LENGTH_LONG).show()
        }
    }
}
