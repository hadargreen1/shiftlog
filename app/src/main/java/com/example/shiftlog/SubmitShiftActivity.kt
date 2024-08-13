package com.example.shiftlog

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
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
                Toast.makeText(this, "Shift started. Press again to stop.", Toast.LENGTH_SHORT)
                    .show()
            } else {
                // End the shift and record the end time
                endTime = System.currentTimeMillis()
                endTimeInput.setText(getFormattedTime(endTime))
                isShiftStarted = false  // Reset the flag for the next shift
                Toast.makeText(this, "Shift ended. Now, press Submit to save.", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        submitShiftButton.setOnClickListener {
            // Get the input values
            val startTimeText = startTimeInput.text.toString()
            val endTimeText = endTimeInput.text.toString()

            // Validate inputs
            if (startTimeText.isEmpty() || endTimeText.isEmpty()) {
                Toast.makeText(this, "Please enter both start and end times.", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            // Convert times to timestamps if manually entered
            startTime = getTimestampFromTime(startTimeText)
            endTime = getTimestampFromTime(endTimeText)

            // Calculate duration and salary
            val durationHours = calculateDuration(startTime, endTime)
            val salary = calculateSalary(durationHours)

            // Debugging logs
            Log.d("SubmitShiftActivity", "Start Time: $startTimeText, End Time: $endTimeText")
            Log.d("SubmitShiftActivity", "Duration: $durationHours hours, Salary: $$salary")

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

    // In SubmitShiftActivity
    private fun saveShiftData(
        startTime: String,
        endTime: String,
        duration: Double,
        salary: Double
    ) {
        val user = auth.currentUser?.uid
        val db = Firebase.firestore

        if (user != null) {
            // Get the current date
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            // Reference to the user's document and specific date
            val userDocRef =
                db.collection("users").document(user).collection("shifts").document(currentDate)

            // Show loading indicator
            showLoadingIndicator(true)

            userDocRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    // Update existing document for the current date
                    updateTimes(document, startTime, endTime, duration, salary, userDocRef)
                } else {
                    // Initialize new document for the current date
                    initDocument(userDocRef, startTime, endTime, duration, salary)
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Error getting document: ${it.message}", Toast.LENGTH_LONG)
                    .show()
                showLoadingIndicator(false)
            }
        } else {
            Toast.makeText(this, "User is not authenticated.", Toast.LENGTH_LONG).show()
        }
    }

    private fun initDocument(userDocRef: DocumentReference, startTime: String, endTime: String, duration: Double, salary: Double) {
        val shiftData = hashMapOf(
            "startTime" to startTime,
            "endTime" to endTime,
            "duration" to duration,
            "salary" to salary
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

    private fun updateTimes(document: DocumentSnapshot, startTime: String, endTime: String, duration: Double, salary: Double, userDocRef: DocumentReference) {
        val newShiftData = hashMapOf(
            "startTime" to startTime,
            "endTime" to endTime,
            "duration" to duration,
            "salary" to salary
        )
        // Convert existing data to a list of maps
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


    // Function to show/hide a loading indicator
    private fun showLoadingIndicator(show: Boolean) {
        // Show or hide a ProgressBar or some UI element to indicate loading
    }

    // Function to fetch data again and update UI (e.g., refresh calendar view)
    private fun fetchDataAndUpdateUI() {
        // Implementation to fetch data and update the calendar or other UI components
    }
}