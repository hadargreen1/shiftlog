package com.example.shiftlog

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var auth: FirebaseAuth
    private lateinit var daysWorkedTextView: TextView
    private lateinit var salaryGainedTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        navView.setNavigationItemSelectedListener(this)

        val headerView = navView.getHeaderView(0)
        val userNameTextView = headerView.findViewById<TextView>(R.id.nav_user_name)
        val userEmailTextView = headerView.findViewById<TextView>(R.id.nav_user_email)

        // Set user details here
        userNameTextView.text = "John Doe"
        userEmailTextView.text = "john.doe@example.com"

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        daysWorkedTextView = findViewById(R.id.daysWorkedTextView)
        salaryGainedTextView = findViewById(R.id.salaryGainedTextView)

        // Fetch and update the data
        fetchDaysWorkedAndSalary()

        // Load the default fragment when the activity starts
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, CalendarFragment())
                .commit()
            navView.setCheckedItem(R.id.nav_home)
        }
    }

    private fun fetchDaysWorkedAndSalary() {
        val user = auth.currentUser?.uid
        val db = Firebase.firestore

        if (user != null) {
            db.collection("users").document(user).collection("shifts")
                .get()
                .addOnSuccessListener { documents ->
                    var totalDaysWorked = 0
                    var totalHoursWorked = 0.0
                    var totalSalary = 0.0

                    if (!documents.isEmpty) {
                        for (document in documents) {
                            val documentId = document.id

                            // Only consider documents within the current month
                            if (documentId.startsWith("2024-08")) {
                                totalDaysWorked += 1
                                val shiftArray = document.get("shiftArray") as? List<*>
                                shiftArray?.forEach { shift ->
                                    val shiftMap = shift as? Map<*, *>
                                    shiftMap?.let {
                                        val duration = it["duration"] as? Double ?: 0.0
                                        totalHoursWorked += duration

                                        val salary = it["salary"] as? Double ?: 0.0
                                        totalSalary += salary
                                    }
                                }
                            }
                        }

                        Log.d("MainActivity", "Total Days Worked: $totalDaysWorked, Total Hours Worked: $totalHoursWorked, Total Salary: $totalSalary")

                        // Update the UI with the fetched data
                        daysWorkedTextView.text = "Days Worked: $totalDaysWorked"
                        salaryGainedTextView.text = "Salary Gained: $${String.format("%.2f", totalSalary)}"
                    } else {
                        Log.d("MainActivity", "No documents found.")
                        daysWorkedTextView.text = "Days Worked: 0"
                        salaryGainedTextView.text = "Salary Gained: $0.00"
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MainActivity", "Error fetching data: ${e.message}")
                    Snackbar.make(drawerLayout, "Error fetching data: ${e.message}", Snackbar.LENGTH_LONG).show()
                }
        } else {
            Log.e("MainActivity", "User is not authenticated")
        }
    }


    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(androidx.core.view.GravityCompat.START)) {
            drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, CalendarFragment())
                    .commit()
            }
            R.id.nav_submit_shift -> {
                // Navigate to com.example.shiftlog.com.example.shiftlog.com.example.shiftlog.com.example.shiftlog.com.example.shiftlog.SubmitShiftActivity
                val intent = Intent(this, SubmitShiftActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_share -> {
                Snackbar.make(findViewById(R.id.fragment_container), "Achievements clicked", Snackbar.LENGTH_LONG).show()
            }
            R.id.nav_info -> {
                Snackbar.make(findViewById(R.id.fragment_container), "Account Info clicked", Snackbar.LENGTH_LONG).show()
            }
            R.id.nav_logout -> {
                // Log out the user
                auth.signOut()

                // Navigate to the sign-in page
                val intent = Intent(this, StartActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish() // Finish the current activity
            }
        }
        drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START)
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                Snackbar.make(findViewById(R.id.fragment_container), "Settings clicked", Snackbar.LENGTH_LONG).show()
                true
            }
            R.id.action_profile -> {
                Snackbar.make(findViewById(R.id.fragment_container), "Profile clicked", Snackbar.LENGTH_LONG).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
