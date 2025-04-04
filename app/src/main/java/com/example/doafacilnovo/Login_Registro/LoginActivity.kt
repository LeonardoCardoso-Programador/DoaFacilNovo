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
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

class LoginActivity : AppCompatActivity() {

    private val binding by lazy { ActivityLoginBinding.inflate(layoutInflater) }
    private val auth = FirebaseAuth.getInstance()
    private lateinit var sharedPreferences: SharedPreferences
    private val firestore = FirebaseFirestore.getInstance()  // Inicializa o Firestore



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
        carregarCredenciais()


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
                            val uid = auth.currentUser?.uid

                            if (uid != null) {
                                saveUserUid(uid)

                                // Verifica se o checkbox está marcado para salvar credenciais
                                if (binding.checkBoxLembrar.isChecked) {
                                    salvarCredenciais(email, pass)
                                } else {
                                    limparCredenciais()
                                }

                                Toast.makeText(this, "Login realizado com sucesso", Toast.LENGTH_SHORT).show()

                                val intent = Intent(this, MainActivity::class.java)
                                intent.putExtra("USER_UID", uid)
                                startActivity(intent)
                            }
                        }
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(this, "Erro ao realizar login: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun salvarCredenciais(email: String, senha: String) {
        val editor = sharedPreferences.edit()
        editor.putString("EMAIL", email)
        editor.putString("SENHA", senha)
        editor.putBoolean("LEMBRAR", true)
        editor.apply()
    }

    private fun carregarCredenciais() {
        val email = sharedPreferences.getString("EMAIL", "")
        val senha = sharedPreferences.getString("SENHA", "")
        val lembrar = sharedPreferences.getBoolean("LEMBRAR", false)

        if (lembrar) {
            binding.editEmail.setText(email)
            binding.editPass.setText(senha)
            binding.checkBoxLembrar.isChecked = true
        }
    }

    private fun limparCredenciais() {
        val editor = sharedPreferences.edit()
        editor.remove("EMAIL")
        editor.remove("SENHA")
        editor.putBoolean("LEMBRAR", false)
        editor.apply()
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



    private fun verificarExpiracaoDoacoes() {
        val agora = Date() // Pega a data e hora atuais

        firestore.collection("doacoes")
            .whereEqualTo("disponibilidade", "disponível") // Só busca as disponíveis
            .get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot.documents) {
                    val dataExpiracao = document.getDate("dataExpiracao")

                    if (dataExpiracao != null && dataExpiracao.before(agora)) {
                        // Se a data de expiração já passou, atualiza o status para "indisponível"
                        firestore.collection("doacoes").document(document.id)
                            .update("disponibilidade", "indisponível")
                            .addOnSuccessListener {
                                Log.d("Expiração", "Doação ${document.id} marcada como indisponível")
                            }
                            .addOnFailureListener { e ->
                                Log.e("Expiração", "Erro ao atualizar a doação: ${e.message}")
                            }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Expiração", "Erro ao buscar doações: ${e.message}")
            }
    }

    override fun onStart() {
        super.onStart()
        verificarExpiracaoDoacoes() // Atualiza as doações expiradas ao iniciar a tela
    }

}
