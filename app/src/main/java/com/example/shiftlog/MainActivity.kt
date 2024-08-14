package com.example.shiftlog

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var daysWorkedTextView: TextView
    private lateinit var salaryGainedTextView: TextView
    private lateinit var userNameTextView: TextView
    private lateinit var userEmailTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        navView.setNavigationItemSelectedListener(this)

        val headerView = navView.getHeaderView(0)
        userNameTextView = headerView.findViewById(R.id.nav_user_name)
        userEmailTextView = headerView.findViewById(R.id.nav_user_email)

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

    @SuppressLint("SetTextI18n")
    private fun fetchDaysWorkedAndSalary() {
        val user = auth.currentUser?.uid
        val db = FirebaseDatabase.getInstance("https://shiftlog-6a430-default-rtdb.europe-west1.firebasedatabase.app").reference
        val firestore = FirebaseFirestore.getInstance()

        if (user != null) {
            val userRef = db.child("users").child(user).child("shifts")
            val userFirestoreRef = firestore.collection("users").document(user)

            // Fetch shift data
            userRef.addValueEventListener(object : ValueEventListener {
                @SuppressLint("SetTextI18n", "DefaultLocale")
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    var totalDaysWorked = 0
                    var totalHoursWorked = 0.0
                    var totalSalary = 0.0

                    val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

                    for (dateSnapshot in dataSnapshot.children) {
                        if (dateSnapshot.key?.startsWith(currentMonth) == true) {
                            totalDaysWorked += 1
                            for (shiftSnapshot in dateSnapshot.children) {
                                val duration = shiftSnapshot.child("duration").getValue(Double::class.java) ?: 0.0
                                val salary = shiftSnapshot.child("salary").getValue(Double::class.java) ?: 0.0
                                totalHoursWorked += duration
                                totalSalary += salary
                            }
                        }
                    }

                    // Update UI with shift data
                    runOnUiThread {
                        daysWorkedTextView.text = "Days Worked: $totalDaysWorked"
                        salaryGainedTextView.text = "Salary Gained: $${String.format("%.2f", totalSalary)}"
                    }


                    userFirestoreRef.get().addOnSuccessListener { document ->
                        if (document != null && document.exists()) {
                            val userName = document.getString("fullName") ?: "N/A"
                            val userEmail = document.getString("email") ?: "N/A"

                            // Update UI with user data
                            runOnUiThread {
                                userNameTextView.text = "Name: $userName"
                                userEmailTextView.text = "Email: $userEmail"
                            }
                        } else {
                            // Handle case where document does not exist
                            runOnUiThread {
                                userNameTextView.text = "No user data found"
                                userEmailTextView.text = "No user data found"
                            }
                        }
                    }.addOnFailureListener {
                        // Handle possible errors
                        runOnUiThread {
                            userNameTextView.text = "Error fetching data"
                            userEmailTextView.text = "Error fetching data"
                        }
                    }
                }

                @SuppressLint("SetTextI18n")
                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle possible errors
                    runOnUiThread {
                        daysWorkedTextView.text = "Error fetching shift data"
                        salaryGainedTextView.text = "Error fetching shift data"
                    }
                }
            })
        } else {
            // Handle case where user is not logged in
            runOnUiThread {
                daysWorkedTextView.text = "No user logged in"
                salaryGainedTextView.text = "No user logged in"
                userNameTextView.text = "No user logged in"
                userEmailTextView.text = "No user logged in"
            }
        }
    }

    @Deprecated("Deprecated in Java")
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
                val intent = Intent(this, SubmitShiftActivity::class.java)
                startActivity(intent)
            }
            R.id.pay_management -> {
                val intent = Intent(this, PayManagementActivity::class.java)
                startActivity(intent)}
            R.id.nav_info -> {
                Snackbar.make(findViewById(R.id.fragment_container), "Account Info clicked", Snackbar.LENGTH_LONG).show()
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
