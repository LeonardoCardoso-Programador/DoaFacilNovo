package com.example.doafacilnovo

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.doafacilnovo.Login_Registro.LoginActivity
import com.example.doafacilnovo.databinding.ActivitySolicitacoesFeitasBinding
import com.example.doafacilnovo.databinding.ActivitySolicitacoesRecebidasBinding
import com.google.android.material.navigation.NavigationView
import com.google.firebase.firestore.FirebaseFirestore

class SolicitacoesFeitasActivity : AppCompatActivity() {

    private val binding by lazy { ActivitySolicitacoesFeitasBinding.inflate(layoutInflater) }
    private val db = FirebaseFirestore.getInstance()  // Inicializa o Firestore
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var userUid: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        //Chamando sharedPreferences
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        userUid = getUserUid() ?: ""

        // Inicializando o DrawerLayout e NavigationView
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)

        // Configuração do RecyclerView usando o binding
        binding.recyclerSolicitacoes.layoutManager = LinearLayoutManager(this)

        // Ação do botão Menu (abre o DrawerLayout à direita)
        binding.ButtonMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)  // Abrindo o menu à direita
        }

        // Configurando o listener do NavigationView para os itens do menu
        navigationView.setNavigationItemSelectedListener { item ->
            handleNavigationItemSelected(item)
            drawerLayout.closeDrawer(GravityCompat.END)  // Fechando o menu ao selecionar um item
            true
        }
        fetchSolicitacoesFeitas()

    }

    private fun getUserUid(): String? {
        return sharedPreferences.getString("USER_UID", null)
    }

    // Função que trata as ações de navegação
    private fun handleNavigationItemSelected(item: MenuItem) {
        val userUid = intent.getStringExtra("USER_UID") ?: "UID nao disponivel"
        when (item.itemId) {
            R.id.nav_home -> {
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("USER_UID", userUid)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
                finish()
            }

            R.id.nav_doacoes -> {
                val intent = Intent(this, MinhaDoacaoActitity::class.java)
                intent.putExtra("USER_UID", userUid)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
                finish()
            }

            R.id.nav_MapaLoc -> {
                val intent = Intent(this, LocalizacaoActivity::class.java)
                intent.putExtra("USER_UID", userUid)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
                finish()
            }

            R.id.nav_Perfil -> {
                val intent = Intent(this, PerfilActivity::class.java)
                intent.putExtra("USER_UID", userUid)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
                finish()
            }
            R.id.nav_Vizualizar_Perfil -> {
                val intent = Intent(this, VizualizarPerfilActivity::class.java)
                intent.putExtra("USER_UID", userUid)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
                finish()

            }
            R.id.nav_Solicitacoes -> {
                val intent = Intent(this, SolicitacoesRecebidasActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
                finish()
            }
            R.id.nav_SolicitacoesFeitas -> {
            }
            R.id.nav_Logout ->{
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }

            else -> {
                // Caso não haja nenhuma ação definida para o item, nada acontece
            }
        }
    }

    private fun fetchSolicitacoesFeitas() {

        db.collection("solicitacaoDoacao").whereEqualTo("userUidBeneficiario",userUid)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    Log.w("Solicitação", "Nenhuma solicitação encontrada.")
                    return@addOnSuccessListener
                }

                val solicitacoes = mutableListOf<Solicitacoes>() // Lista final

                for (document in result) {
                    val uidSolicitacao = document.id
                    val uidDoacao = document.getString("idDoacaoSolicitada").toString()
                    val uidBeneficiario = document.getString("userUidBeneficiario").toString()

                    // Busca o título da doação
                    db.collection("doacoes").document(uidDoacao).get()
                        .addOnSuccessListener { doacaoDoc ->
                            val tituloDoacao = doacaoDoc.getString("titulo").orEmpty()

                            // Busca o nome do beneficiário
                            db.collection("users").document(uidBeneficiario).get()
                                .addOnSuccessListener { userDoc ->
                                    val nomeBeneficiario = userDoc.getString("nome").orEmpty()

                                    // Criar objeto e adicionar na lista
                                    val solicitacao = Solicitacoes("Você", "Solicitação enviada: " + tituloDoacao, uidSolicitacao)
                                    solicitacoes.add(solicitacao)

                                    // Atualiza RecyclerView somente depois de todas as buscas
                                    if (solicitacoes.size == result.size()) {
                                        updateRecyclerView(solicitacoes)
                                    }
                                }
                                .addOnFailureListener { Log.e("Firestore", "Erro ao buscar usuário.") }
                        }
                        .addOnFailureListener { Log.e("Firestore", "Erro ao buscar doação.") }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Erro ao buscar solicitações: ${exception.message}")
                Toast.makeText(this, "Erro ao carregar os dados", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateRecyclerView(solicitacoes: List<Solicitacoes>) {
        val adapter = ListaSolicitacao(this, solicitacoes) { solicitacao -> // Correção do nome da variável
            val intent = Intent(this, VizualizarSolicitacaoFeita::class.java).apply {
                putExtra("solicitacaoUid", solicitacao.solicitacaoUid) // Passando o nome do usuário
            }
            startActivity(intent)
        }
        binding.recyclerSolicitacoes.adapter = adapter
    }
}