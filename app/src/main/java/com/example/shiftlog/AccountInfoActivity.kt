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

class AccountInfoActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var wageRateEditText: EditText
    private lateinit var saveButton: Button

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
        saveButton = findViewById(R.id.saveButton)

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
        val userFirestoreRef = firestore.collection("users").document(user!!)

        userFirestoreRef.get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val fullName = document.getString("fullName") ?: "N/A"
                val email = document.getString("email") ?: "N/A"
                val wageRate = document.getString("wageRate") ?: "N/A"

                findViewById<TextView>(R.id.fullNameTextView).text = "Full Name: $fullName"
                findViewById<TextView>(R.id.emailTextView).text = "Email: $email"
                wageRateEditText.setText(wageRate)
            }
        }
    }

    private fun saveWageRate() {
        val user = auth.currentUser?.uid
        val newWageRate = wageRateEditText.text.toString()

        if (user != null) {
            val userFirestoreRef = firestore.collection("users").document(user)
            userFirestoreRef.update("wageRate", newWageRate)
                .addOnSuccessListener {
                    // Wage rate updated successfully
                    loadUserInfo()  // Refresh the user info
                }
                .addOnFailureListener {
                    // Handle the error
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
