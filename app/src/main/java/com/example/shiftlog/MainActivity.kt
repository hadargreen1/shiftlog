package com.example.shiftlog

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

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var auth: FirebaseAuth

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

        // Load the default fragment when the activity starts
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, CalendarFragment())
                .commit()
            navView.setCheckedItem(R.id.nav_home)
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
                // Navigate to SubmitShiftActivity
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
