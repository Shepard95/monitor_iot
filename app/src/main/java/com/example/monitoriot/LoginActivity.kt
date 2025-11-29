package com.example.monitoriot

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.lifecycle.lifecycleScope
import com.example.monitoriot.databinding.ActivityLoginBinding
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var credentialManager: CredentialManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        db = Firebase.firestore
        credentialManager = CredentialManager.create(this)

        if (auth.currentUser != null) {
            navigateToMain()
            return
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.btnLogin.setOnClickListener { loginWithEmailPassword() }
        binding.btnGoogleSignIn.setOnClickListener { signInWithGoogleButton() }
        binding.btnGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        showGoogleSignInBottomSheet()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun loginWithEmailPassword() {
        val email = binding.txtEmail.text.toString().trim()
        val password = binding.txtPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Completa ambos campos", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    navigateToMain()
                } else {
                    handleEmailPasswordAuthError(task.exception)
                }
            }
    }

    private fun showGoogleSignInBottomSheet() {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(getString(R.string.default_web_client_id))
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    context = this@LoginActivity,
                    request = request
                )
                handleSignInResult(result)
            } catch (e: Exception) {
                Log.e(TAG, "Error en Bottom Sheet de sign in", e)
            }
        }
    }

    private fun signInWithGoogleButton() {
        val signInWithGoogleOption = GetSignInWithGoogleOption
            .Builder(serverClientId = getString(R.string.default_web_client_id))
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    context = this@LoginActivity,
                    request = request
                )
                handleSignInResult(result)
            } catch (e: Exception) {
                Log.e(TAG, "Error al iniciar sesión con Google", e)
                Toast.makeText(
                    this@LoginActivity,
                    "Error al iniciar sesión",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun handleSignInResult(result: GetCredentialResponse) {
        val credential = result.credential

        if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken

                firebaseAuthWithGoogle(idToken)
            } catch (e: GoogleIdTokenParsingException) {
                Log.e(TAG, "Token inválido recibido", e)
                Toast.makeText(this, "Error al procesar token", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.e(TAG, "Tipo inesperado de credencial")
            Toast.makeText(this, "Tipo de credencial no soportado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser!!
                    val isNewUser = task.result?.additionalUserInfo?.isNewUser == true

                    if (isNewUser) {
                        createOrUpdateUserInFirestore(firebaseUser)
                    }
                    navigateToMain()
                } else {
                    handleGoogleAuthError(task.exception)
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
                Log.d(TAG, "usuario nuevo guardado en firestore: ${firebaseUser.uid}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "error guardando usuario nuevo en firestore", e)
            }
    }

    private fun handleEmailPasswordAuthError(exception: Exception?) {
        Toast.makeText(this, "error en la autenticación: ${exception?.localizedMessage}", Toast.LENGTH_SHORT).show()
    }

    private fun handleGoogleAuthError(exception: Exception?) {
        val ex = exception as? FirebaseAuthException
        when (ex?.errorCode) {
            "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> {
                val email = parseEmailFromError(ex.message)
                if (email != null) {
                    binding.txtEmail.setText(email)
                    binding.txtEmail.requestFocus()
                }
                Toast.makeText(this, "Usa email/password para esta cuenta.", Toast.LENGTH_LONG).show()
            }

            "ERROR_EMAIL_ALREADY_IN_USE" -> {
                Toast.makeText(this, "Esta cuenta ya está registrada con Google.", Toast.LENGTH_SHORT).show()
            }

            else -> {
                Toast.makeText(this, "Error: ${exception?.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun parseEmailFromError(errorMessage: String?): String? {
        errorMessage?.let {
            val regex = Regex("address\\s*[:]\\s*([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})")
            val match = regex.find(it)
            return match?.groupValues?.get(1)?.trim()
        }
        return null
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK.or(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }

    companion object {
        private const val TAG = "LoginActivity"
    }
}
