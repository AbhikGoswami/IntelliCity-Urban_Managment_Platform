package com.abhik.urbanmanagementplatform.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.abhik.urbanmanagementplatform.R
import com.abhik.urbanmanagementplatform.fragments.HomeFragment
import com.abhik.urbanmanagementplatform.fragments.ProfileFragment
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        val navigationView: NavigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        // This sets up the hamburger menu icon
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // This block ensures that the app starts on the HomeFragment
        // and doesn't create a new fragment every time the screen rotates.
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment()).commit()
            navigationView.setCheckedItem(R.id.nav_home)
        }

        updateNavHeader()
    }

    // This function fetches the user's name from Firestore and displays it in the nav drawer header.
    private fun updateNavHeader() {
        val navigationView: NavigationView = findViewById(R.id.nav_view)
        val headerView = navigationView.getHeaderView(0)
        val navUsername = headerView.findViewById<TextView>(R.id.nav_header_name)
        val navUserEmail = headerView.findViewById<TextView>(R.id.nav_header_email)

        val currentUser = auth.currentUser
        if (currentUser != null) {
            navUserEmail.text = currentUser.email
            // Fetch user's name from Firestore
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        navUsername.text = document.getString("name") ?: "No Name"
                    }
                }
        }
    }

    // This function handles clicks on items in the navigation drawer.
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, HomeFragment()).commit()
            }
            R.id.nav_profile -> {
                // UPDATED: Navigate to the ProfileFragment
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, ProfileFragment())
                    .addToBackStack(null) // Allows user to press back to return to home
                    .commit()
            }
            R.id.nav_track_status -> {
                // TODO: We will create and navigate to TrackStatusFragment in a later phase
            }
            // ... other navigation items ...
            R.id.nav_logout -> {
                auth.signOut()
                val intent = Intent(this, AuthActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish() // Close this activity
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    // This handles the back button press to close the drawer if it's open.
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
