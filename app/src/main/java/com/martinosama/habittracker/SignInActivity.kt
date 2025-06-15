package com.martinosama.habittracker

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.martinosama.habittracker.backend.RetrofitInstance
import com.martinosama.habittracker.backend.UserRepository
import com.martinosama.habittracker.model.UserDto
import kotlinx.coroutines.launch

class SignInActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient
    private val userRepository = UserRepository(RetrofitInstance.userApi)

    companion object {
        private const val RC_SIGN_IN = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val signInButton = findViewById<Button>(R.id.signInButton)
        val signUpText = findViewById<TextView>(R.id.signUpText)
        val customGoogleSignInBtn = findViewById<View>(R.id.customGoogleSignInButton)

        signInButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            if (user != null) {
                                db.collection("users").document(user.uid)
                                    .get()
                                    .addOnSuccessListener { doc ->
                                        val profile = doc.toObject(UserProfile::class.java)
                                        val firstName = profile?.firstName?.takeIf { it.isNotBlank() }
                                            ?: "User"

                                        val intent = Intent(this, MainActivity::class.java)
                                        intent.putExtra("EXTRA_USER_NAME", firstName)
                                        startActivity(intent)
                                        finish()

                                    }.addOnFailureListener {
                                        val intent = Intent(this, MainActivity::class.java)
                                        intent.putExtra("EXTRA_USER_NAME", "User")
                                        startActivity(intent)
                                        finish()
                                    }
                            } else {
                                val intent = Intent(this, MainActivity::class.java)
                                intent.putExtra("EXTRA_USER_NAME", "User")
                                startActivity(intent)
                                finish()
                            }
                        } else {
                            Toast.makeText(
                                this,
                                "❌ Sign-in failed: ${task.exception?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            } else {
                Toast.makeText(this, "⚠️ Enter both email and password.", Toast.LENGTH_SHORT).show()
            }
        }

        customGoogleSignInBtn.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            signInLauncher.launch(signInIntent)
        }

        signUpText.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            if (task.isSuccessful) {
                firebaseAuthWithGoogle(task.result)
            } else {
                Toast.makeText(this, "Google Sign-In Failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Deprecated("Deprecated API, use ActivityResultLauncher instead")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            if (task.isSuccessful) {
                val account = task.result
                firebaseAuthWithGoogle(account)
            } else {
                Toast.makeText(
                    this,
                    "❌ Google Sign-In Failed: ${task.exception?.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {
        val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        val displayName = user.displayName ?: "User"
                        val photoUrl = user.photoUrl
                        val nameParts = displayName.split(" ")
                        val firstName = nameParts.getOrElse(0) { "" }
                        val lastName = nameParts.drop(1).joinToString(" ")

                        val partialMap = mapOf(
                            "firstName" to firstName,
                            "lastName" to lastName,
                            "avatarUrl" to (photoUrl?.toString().orEmpty())
                        )

                        lifecycleScope.launch {
                            try {
                                val userDto = UserDto(
                                    firstName = firstName,
                                    lastName = lastName,
                                    birthday = "",
                                    gender = "",
                                    profileImageUrl = photoUrl?.toString()
                                )
                                userRepository.createUser(user.uid, user.email ?: "", userDto)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(this@SignInActivity, "Failed to create user in backend", Toast.LENGTH_SHORT).show()
                            }
                        }

                        db.collection("users").document(user.uid)
                            .set(partialMap, SetOptions.merge())
                            .addOnSuccessListener {
                                db.collection("users").document(user.uid)
                                    .get()
                                    .addOnSuccessListener { doc ->
                                        val profile = doc.toObject(UserProfile::class.java)
                                        val finalFirstName = profile?.firstName?.takeIf { it.isNotBlank() } ?: "User"

                                        val intent = Intent(this, MainActivity::class.java)
                                        intent.putExtra("EXTRA_USER_NAME", finalFirstName)
                                        startActivity(intent)
                                        finish()
                                    }
                                    .addOnFailureListener {
                                        val intent = Intent(this, MainActivity::class.java)
                                        intent.putExtra("EXTRA_USER_NAME", "User")
                                        startActivity(intent)
                                        finish()
                                    }
                            }
                            .addOnFailureListener {
                                val intent = Intent(this, MainActivity::class.java)
                                intent.putExtra("EXTRA_USER_NAME", "User")
                                startActivity(intent)
                                finish()
                            }
                    } else {
                        val intent = Intent(this, MainActivity::class.java)
                        intent.putExtra("EXTRA_USER_NAME", "User")
                        startActivity(intent)
                        finish()
                    }
                } else {
                    Toast.makeText(
                        this,
                        "❌ Google Sign-In failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
}