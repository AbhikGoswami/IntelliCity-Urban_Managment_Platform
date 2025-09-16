package com.abhik.urbanmanagementplatform.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.abhik.urbanmanagementplatform.R
import com.abhik.urbanmanagementplatform.fragments.LoginFragment
import com.google.firebase.auth.FirebaseAuth

class AuthActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.authactivitylayout)

//        auth = FirebaseAuth.getInstance()
//
//        // Check if user is already logged in
//        if (auth.currentUser != null) {
//            // User is signed in, so navigate to MainActivity
//            startActivity(Intent(this, MainActivity::class.java))
//            finish()
//            return
//        }

        // If no user, show the LoginFragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LoginFragment())
                .commit()
        }
    }
}
