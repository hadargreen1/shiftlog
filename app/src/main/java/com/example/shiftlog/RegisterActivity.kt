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

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var dobInput: EditText
    private lateinit var registerButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        dobInput = findViewById(R.id.dobInput)
        registerButton = findViewById(R.id.registerButton)

        registerButton.setOnClickListener {
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()
            val dob = dobInput.text.toString()
            registerUser(email, password, dob)
        }
    }

    private fun registerUser(email: String, password: String, dob: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Save additional user data to Firebase Database
                    saveUserData(email, dob)

                    Toast.makeText(this, "Registration successful.", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    val errorMessage = when (task.exception) {
                        is FirebaseAuthUserCollisionException -> "This email address is already in use."
                        else -> "Registration failed: ${task.exception?.message}"
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }


    private fun saveUserData(email: String, dob: String) {
        val user = auth.currentUser
        val uid = user?.uid

        val database = FirebaseDatabase.getInstance()
        val usersRef = database.getReference("users")

        val userData = HashMap<String, String>()
        userData["email"] = email
        userData["dob"] = dob

        if (uid != null) {
            usersRef.child(uid).setValue(userData)
        }
    }
}
