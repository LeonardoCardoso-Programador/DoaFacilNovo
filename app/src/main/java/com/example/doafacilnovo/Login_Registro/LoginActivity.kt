package com.example.doafacilnovo.Login_Registro

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.doafacilnovo.MainActivity
import com.example.doafacilnovo.MinhaDoacaoActitity
import com.example.doafacilnovo.R
import com.example.doafacilnovo.databinding.ActivityLoginBinding
import com.example.doafacilnovo.databinding.ActivityMainBinding
import com.google.android.play.integrity.internal.l
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private val binding by lazy { ActivityLoginBinding.inflate(layoutInflater) }
    private val auth = FirebaseAuth.getInstance()
    private lateinit var sharedPreferences: SharedPreferences


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        // Inicializar SharedPreferences
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        // Recupera o UID do usuário salvo nas SharedPreferences
        val storedUid = getUserUid()
        Log.d("LoginActivity", "UID recuperado das SharedPreferences: $storedUid")
        if (storedUid == "UID não encontrado") {
            Log.d("LoginActivity", "Nenhum UID encontrado nas SharedPreferences.")
        } else {
            Log.d("LoginActivity", "UID recuperado: $storedUid")
        }

        binding.buttonRegistro.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        binding.buttonEntrar.setOnClickListener {
            val email = binding.editEmail.text.toString()
            val pass = binding.editPass.text.toString()

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            } else {
                auth.signInWithEmailAndPassword(email, pass)
                    .addOnCompleteListener { login ->
                        if (login.isSuccessful) {
                            // Obtendo o UID do usuário autenticado
                            val uid = auth.currentUser?.uid

                            if (uid != null) {
                                // Salva o UID no SharedPreferences
                                Log.d("LoginActivity", "UID obtido após login: $uid")
                                saveUserUid(uid)

                                // Exibe mensagem de sucesso
                                Toast.makeText(this, "Login realizado com sucesso", Toast.LENGTH_SHORT).show()

                                // Navega para a MainActivity
                                val intent = Intent(this, MainActivity::class.java)
                                intent.putExtra("USER_UID", uid)
                                startActivity(intent)
                                finish()
                            } else {
                                Log.d("LoginActivity", "UID é nulo após login.")
                            }
                        }
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(this, "Erro ao realizar login: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    // Salva o UID do usuário nas SharedPreferences
    private fun saveUserUid(uid: String) {
        val editor = sharedPreferences.edit()
        editor.putString("USER_UID", uid)
        editor.apply() // Salva de forma assíncrona
        Log.d("LoginActivity", "UID salvo nas SharedPreferences: $uid")
    }

    // Recupera o UID do usuário das SharedPreferences
    private fun getUserUid(): String {
        return sharedPreferences.getString("USER_UID", "UID não encontrado") ?: "UID não encontrado"
    }
}
