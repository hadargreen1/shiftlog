package com.example.shiftlog

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.database.FirebaseDatabase
import android.widget.Toast

class AccountInfoActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var hourlyWageEditText: EditText
    private lateinit var wageRateEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var hourlyWageTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_info)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        navView.setNavigationItemSelectedListener(this)

        // Setup the drawer toggle button
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Initialize UI elements
        wageRateEditText = findViewById(R.id.wageRateEditText)
        hourlyWageEditText = findViewById(R.id.wageRateEditText) // EditText for the hourly wage input
        saveButton = findViewById(R.id.saveButton)
        hourlyWageTextView = findViewById(R.id.hourlyWageTextView)

        // Load user information
        loadUserInfo()

        // Save button click listener
        saveButton.setOnClickListener {
            saveWageRate()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun loadUserInfo() {
        val user = auth.currentUser?.uid
        if (user != null) {
            val userFirestoreRef = firestore.collection("users").document(user)

            // Listen for real-time updates from Firestore
            userFirestoreRef.addSnapshotListener { documentSnapshot, error ->
                if (error != null) {
                    // Handle the error here
                    return@addSnapshotListener
                }

                if (documentSnapshot != null && documentSnapshot.exists()) {
                    val fullName = documentSnapshot.getString("fullName") ?: "N/A"
                    val email = documentSnapshot.getString("email") ?: "N/A"
                    val wageRate = documentSnapshot.getString("wageRate") ?: "N/A"

                    // Update Firestore UI elements
                    findViewById<TextView>(R.id.fullNameTextView).text = "Full Name: $fullName"
                    findViewById<TextView>(R.id.emailTextView).text = "Email: $email"
                    findViewById<EditText>(R.id.wageRateEditText).setText(wageRate)
                }
            }

            // Fetch the hourly wage from Realtime Database
            val db = FirebaseDatabase.getInstance("https://shiftlog-6a430-default-rtdb.europe-west1.firebasedatabase.app").reference
            val userRef = db.child("users").child(user)

            userRef.child("hourlyWage").get().addOnSuccessListener { dataSnapshot ->
                val hourlyWage = dataSnapshot.getValue(Double::class.java) ?: 0.0
                findViewById<TextView>(R.id.hourlyWageTextView).text = "Hourly Wage: $${String.format("%.2f", hourlyWage)}"
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to fetch hourly wage from Realtime Database", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun saveWageRate() {
        val user = auth.currentUser?.uid
        val newWageRate = wageRateEditText.text.toString()
        val newHourlyWage = hourlyWageEditText.text.toString().toDoubleOrNull() ?: 0.0

        if (user != null) {
            // Save to Firestore
            val userFirestoreRef = firestore.collection("users").document(user)
            userFirestoreRef.update("wageRate", newWageRate, "hourlyWage", newHourlyWage)
                .addOnSuccessListener {
                    // Wage rate and hourly wage updated successfully in Firestore
                    loadUserInfo()  // Refresh the user info
                }
                .addOnFailureListener {
                    // Handle the error
                    Toast.makeText(this, "Failed to save wage rate in Firestore", Toast.LENGTH_SHORT).show()
                }

            // Save to Realtime Database
            val db = FirebaseDatabase.getInstance("https://shiftlog-6a430-default-rtdb.europe-west1.firebasedatabase.app").reference
            val userRef = db.child("users").child(user)
            userRef.child("hourlyWage").setValue(newHourlyWage)
                .addOnSuccessListener {
                    // Wage rate updated successfully in Realtime Database
                    Toast.makeText(this, "Hourly wage saved in Realtime Database", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    // Handle the error
                    Toast.makeText(this, "Failed to save hourly wage in Realtime Database", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_submit_shift -> {
                val intent = Intent(this, SubmitShiftActivity::class.java)
                startActivity(intent)
            }
            R.id.pay_management -> {
                val intent = Intent(this, PayManagementActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_info -> {
                val intent = Intent(this, AccountInfoActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_logout -> {
                auth.signOut()
                val intent = Intent(this, StartActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
        drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(androidx.core.view.GravityCompat.START)) {
            drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
