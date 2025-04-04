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
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.doafacilnovo.databinding.ActivityListagensUserBinding
import com.google.android.material.navigation.NavigationView
import com.google.firebase.firestore.FirebaseFirestore

class ListagensUserActivity : AppCompatActivity() {
    private val binding by lazy { ActivityListagensUserBinding.inflate(layoutInflater) }
    private val db = FirebaseFirestore.getInstance()  // Inicializa o Firestore
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    // Declarando SharedPreferences
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var userUid: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)


        // Inicializando SharedPreferences
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        userUid = getUserUid() ?: ""


        // Inicializando o DrawerLayout e NavigationView
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)

        // Configuração do RecyclerView usando o binding
        binding.recyclerUsuarios.layoutManager = LinearLayoutManager(this)

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

        // Buscando os dados dos usuários
        fetchUser()
    }

    // Função para obter o UID do usuário salvo no SharedPreferences
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

            }
            R.id.nav_Solicitacoes -> {
                val intent = Intent(this, SolicitacoesRecebidasActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
                finish()
            }
            R.id.nav_SolicitacoesFeitas -> {
                val intent = Intent(this, SolicitacoesFeitasActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
            }

            else -> {
                // Caso não haja nenhuma ação definida para o item, nada acontece
            }
        }
    }

    private fun fetchUser() {
        db.collection("users").whereNotEqualTo("uid",userUid)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    Log.w("Doação", "Nenhuma doação encontrada.")
                    return@addOnSuccessListener
                }

                // Lista de usuários que será preenchida com os dados do Firestore
                val usuarios = mutableListOf<Usuario>()

                // Iterando sobre os documentos e preenchendo a lista de usuários
                for (document in result) {
                    val nome = document.getString("nome") ?: "Desconhecido"
                    val nDoacoes = document.getDouble("nDoacoes")?.toInt().toString()
                    userUid = document.getString("uid").toString()

                    // Adiciona o usuário à lista
                    usuarios.add(Usuario(nome, "Doações: $nDoacoes", userUid))
                }

                // Atualiza o RecyclerView com os dados
                updateRecyclerView(usuarios)
            }
            .addOnFailureListener { exception ->
                Log.e("Doação", "Erro ao buscar usuários: ${exception.message}")
                Toast.makeText(this, "Erro ao carregar os dados", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateRecyclerView(usuarios: List<Usuario>) {
        val adapter = UserAdapter(this, usuarios) { usuario ->
            val intent = Intent(this, VizualizarPerfilActivity::class.java).apply {
                putExtra("UserSelecionadoUid", usuario.userUid) // Passando o userUid
                putExtra("name", usuario.nome) // Passando o nome do usuário
            }
            startActivity(intent)
        }
        binding.recyclerUsuarios.adapter = adapter
    }
}
