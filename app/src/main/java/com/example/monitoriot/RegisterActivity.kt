package com.example.monitoriot

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.monitoriot.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        auth = Firebase.auth
        db = Firebase.firestore

        binding.btnRegister.setOnClickListener { registerWithEmailPassword() }
        binding.btnGoToLogin.setOnClickListener { navigateToLogin() }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun registerWithEmailPassword() {
        binding.btnRegister.isEnabled = false
        val name = binding.txtName.text.toString().trim()
        val email = binding.txtEmail.text.toString().trim()
        val password = binding.txtPassword.text.toString().trim()

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser!!
                    updateUserProfile(firebaseUser, name)
                } else {
                    binding.btnRegister.isEnabled = true
                    handleRegisterError(task.exception)
                }
            }
    }

    private fun updateUserProfile(firebaseUser: FirebaseUser, name: String) {
        val profileUpdates = userProfileChangeRequest { displayName = name }
        firebaseUser.updateProfile(profileUpdates)
            .addOnCompleteListener(this) { profileTask ->
                if (profileTask.isSuccessful) {
                    createOrUpdateUserInFirestore(firebaseUser)
                    Toast.makeText(this, "Registro exitoso. Ahora puedes iniciar sesión.", Toast.LENGTH_LONG).show()
                    auth.signOut()
                    navigateToLogin()
                } else {
                    Toast.makeText(this, "Error al guardar el nombre de usuario.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun createOrUpdateUserInFirestore(firebaseUser: FirebaseUser) {
        val user = User(
            name = firebaseUser.displayName ?: "",
            email = firebaseUser.email ?: "",
            createdAt = System.currentTimeMillis()
        )

        db.collection("usuarios")
            .document(firebaseUser.uid)
            .set(user, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "Usuario guardado/actualizado en Firestore")
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al guardar datos de usuario: ${e.localizedMessage}", Toast.LENGTH_LONG)
                    .show()
            }
    }

    private fun handleRegisterError(exception: Exception?) {
        val ex = exception as? FirebaseAuthException
        when (ex?.errorCode) {
            "ERROR_EMAIL_ALREADY_IN_USE" -> {
                Toast.makeText(this, "Ese correo ya está registrado.", Toast.LENGTH_SHORT).show()
            }

            else -> {
                Toast.makeText(this, "Falló el registro: ${exception?.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK || Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK || Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    companion object {
        private const val TAG = "RegisterActivity"
    }
}
