package com.app.asap_admin

import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import android.widget.EditText
import android.os.Bundle
import android.widget.TextView
import android.content.Intent
import android.widget.Toast
import com.google.firebase.auth.FirebaseUser
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.Button
import com.app.asap_admin.MainActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etUsername: EditText
    private var username: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        supportActionBar?.hide()

        mAuth = FirebaseAuth.getInstance()
        etUsername = findViewById(R.id.idETUsername)
        etEmail = findViewById(R.id.idETEmail)
        etPassword = findViewById(R.id.idETPassword)

        val linkTextView = findViewById<TextView>(R.id.linkTextView)
        val newAccount = findViewById<TextView>(R.id.new_act)
        val btnLogin = findViewById<Button>(R.id.idBTNLogin)

        linkTextView.setOnClickListener {
            openUrl("https://rohit-prasanna.github.io/APSP_ADMIN_WEB/")
        }

        newAccount.setOnClickListener {
            openUrl("https://rohit-prasanna.github.io/Createuser_APSP/")
        }

        btnLogin.setOnClickListener {
            username = etUsername.text.toString()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            saveUsernameToSharedPreferences(username)
            if (email.isEmpty() || password.isEmpty() || username.isEmpty()) {
                Toast.makeText(this, "Email, password, and username are required", Toast.LENGTH_SHORT).show()
            } else {
                signInWithEmailAndPassword(email, password)
            }
        }
    }

    private fun signInWithEmailAndPassword(email: String, password: String) {
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithEmail: success")
                    updateUI(mAuth.currentUser)
                } else {
                    Log.w(TAG, "signInWithEmail: failure", task.exception)
                    Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                    updateUI(null)
                }
            }
    }

    override fun onStart() {
        super.onStart()
        mAuth.currentUser?.let { updateUI(it) }
    }

    private fun saveUsernameToSharedPreferences(username: String) {
        val preferences = getSharedPreferences("MyPreferences", MODE_PRIVATE)
        preferences.edit().putString("name", username).apply()
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            Toast.makeText(this, "Authentication successful. Welcome, ${user.email}", Toast.LENGTH_SHORT).show()
            Intent(this, MainActivity::class.java).apply {
                putExtra("userid", user.email)
                putExtra("name", username)
                startActivity(this)
            }
            finish()
        } else {
            Toast.makeText(this, "Authentication failure. Please check the credentials.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    companion object {
        private const val TAG = "LoginActivity"
    }
}
