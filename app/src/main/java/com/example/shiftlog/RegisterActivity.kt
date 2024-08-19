package com.example.shiftlog

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var dobInput: EditText
    private lateinit var fullNameInput: EditText
    private lateinit var hourlyWageInput: EditText
    private lateinit var registerButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Link UI elements
        fullNameInput = findViewById(R.id.fullNameInput)
        hourlyWageInput = findViewById(R.id.hourlyWageInput)
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        dobInput = findViewById(R.id.dobInput)
        registerButton = findViewById(R.id.registerButton)

        // Set register button onClick listener
        registerButton.setOnClickListener {
            val fullName = fullNameInput.text.toString()
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()
            val dob = dobInput.text.toString()
            val hourlyWage = hourlyWageInput.text.toString().toDoubleOrNull()

            if (hourlyWage != null) {
                registerUser(fullName, email, password, dob, hourlyWage)
            } else {
                Toast.makeText(this, "Please enter a valid hourly wage.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun registerUser(fullName: String, email: String, password: String, dob: String, hourlyWage: Double) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // If registration is successful, save user data
                    saveUserData(fullName, email, dob, password, hourlyWage)

                    // Show success message and navigate to MainActivity
                    Toast.makeText(this, "Registration successful.", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    // Handle registration failure
                    val errorMessage = when (task.exception) {
                        is FirebaseAuthUserCollisionException -> "This email address is already in use."
                        else -> "Registration failed: ${task.exception?.message}"
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun saveUserData(fullName: String, email: String, dob: String, password: String, hourlyWage: Double) {
        val user = auth.currentUser?.uid
        val firestoreDb = Firebase.firestore
        val realtimeDb = FirebaseDatabase.getInstance("https://shiftlog-6a430-default-rtdb.europe-west1.firebasedatabase.app").reference

        if (user != null) {
            // Create a map for the Firestore user data
            val firestoreUserData = hashMapOf(
                "fullName" to fullName,
                "email" to email,
                "dob" to dob,
                "password" to password
            )

            // Save other user data in Firestore
            firestoreDb.collection("users").document(user).set(firestoreUserData)
                .addOnSuccessListener {
                    // Save the hourly wage in Realtime Database
                    saveHourlyWageToRealtimeDatabase(user, hourlyWage)
                }
                .addOnFailureListener { e ->
                    // Handle any errors in saving user data to Firestore
                    Toast.makeText(this, "Failed to save user data to Firestore: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            Toast.makeText(this, "User is not authenticated.", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveHourlyWageToRealtimeDatabase(userId: String, hourlyWage: Double) {
        val realtimeDb = FirebaseDatabase.getInstance("https://shiftlog-6a430-default-rtdb.europe-west1.firebasedatabase.app").reference
        realtimeDb.child("users").child(userId).child("hourlyWage").setValue(hourlyWage)
            .addOnSuccessListener {
                Toast.makeText(this, "Hourly wage saved successfully.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save hourly wage to Realtime Database: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
