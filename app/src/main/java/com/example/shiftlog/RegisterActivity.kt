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
    private lateinit var registerButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Link UI elements
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        dobInput = findViewById(R.id.dobInput)
        registerButton = findViewById(R.id.registerButton)

        // Set register button onClick listener
        registerButton.setOnClickListener {
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()
            val dob = dobInput.text.toString()
            registerUser(email, password, dob)
        }
    }

    // Method to handle user registration
    private fun registerUser(email: String, password: String, dob: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // If registration is successful, save additional user data
                    saveUserData(email,dob,password)

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

    // Method to save user data to Firebase  Database
    private fun saveUserData(email: String, dob: String, password: String) {
        val user = auth.currentUser?.uid
        val db = Firebase.firestore

        if (user != null) {
            // Create a map for the user data
            val userData = HashMap<String, String>()
            userData["email"] = email
            userData["dob"] = dob
            userData["password"] = password

            // Save user data in Firestore under the user's unique ID
            db.collection("users").document(user).set(userData)
                .addOnSuccessListener {
                    // Clear input fields after successful save
                    passwordInput.text?.clear()
                    dobInput.text?.clear()
                    emailInput.text?.clear()
                    Toast.makeText(this, "User data saved successfully.", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    // Handle any errors in saving user data
                    Toast.makeText(this, "Failed to save user data: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            Toast.makeText(this, "User is not authenticated.", Toast.LENGTH_LONG).show()
        }
    }

}
