import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import com.example.shiftlog.AccountInfoActivity
import com.example.shiftlog.MainActivity
import com.example.shiftlog.PayManagementActivity
import com.example.shiftlog.R
import com.example.shiftlog.StartActivity
import com.example.shiftlog.SubmitShiftActivity
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth

open class BaseActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    protected lateinit var drawerLayout: DrawerLayout
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
    }

    protected fun setupToolbarAndDrawer(toolbarId: Int, drawerLayoutId: Int, navViewId: Int) {
        val toolbar: Toolbar = findViewById(toolbarId)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(drawerLayoutId)
        val navView: NavigationView = findViewById(navViewId)
        navView.setNavigationItemSelectedListener(this)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
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
