package com.martinosama.habittracker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.martinosama.habittracker.backend.RetrofitInstance
import com.martinosama.habittracker.backend.UserRepository
import com.martinosama.habittracker.model.UserDto
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        auth = FirebaseAuth.getInstance()

        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val signUpButton = findViewById<Button>(R.id.signUpButton)
        val signInText = findViewById<TextView>(R.id.signInText)

        signUpButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "✅ Account created successfully!", Toast.LENGTH_SHORT).show()
                        val user = auth.currentUser
                        val userDto = UserDto(
                            firstName = "",
                            lastName = "",
                            birthday = "",
                            gender = "",
                            profileImageUrl = null
                        )

                        val userRepository = UserRepository(RetrofitInstance.userApi)
                        lifecycleScope.launch {
                            try {
                                userRepository.createUser(user?.uid ?: "", user?.email ?: "", userDto)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(this@SignUpActivity, "Failed to sync with backend", Toast.LENGTH_SHORT).show()
                            }
                        }
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "❌ Sign-up failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "⚠️ Enter both email and password.", Toast.LENGTH_SHORT).show()
            }
        }

        signInText.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}