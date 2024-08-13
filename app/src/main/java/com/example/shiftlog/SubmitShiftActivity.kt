package com.example.shiftlog

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

class SubmitShiftActivity : AppCompatActivity() {

    private lateinit var startShiftButton: FloatingActionButton
    private lateinit var startTimeInput: EditText
    private lateinit var endTimeInput: EditText
    private lateinit var submitShiftButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_submit_shift)

        startShiftButton = findViewById(R.id.startShiftButton)
        startTimeInput = findViewById(R.id.startTimeInput)
        endTimeInput = findViewById(R.id.endTimeInput)
        submitShiftButton = findViewById(R.id.submitShiftButton)

        startShiftButton.setOnClickListener {
            // Logic to start shift timer
            Toast.makeText(this, "Shift started", Toast.LENGTH_SHORT).show()
        }

        submitShiftButton.setOnClickListener {
            // Logic to submit the shift times
            val startTime = startTimeInput.text.toString()
            val endTime = endTimeInput.text.toString()

            // You would add your logic to save these times, calculate the duration, and save the shift data

            Toast.makeText(this, "Shift submitted", Toast.LENGTH_SHORT).show()
        }
    }
}
