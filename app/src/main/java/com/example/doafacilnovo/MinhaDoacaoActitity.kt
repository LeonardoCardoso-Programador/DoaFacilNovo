package com.example.doafacilnovo

import CustomAdapter
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.doafacilnovo.Login_Registro.LoginActivity
import com.example.doafacilnovo.databinding.ActivityMinhaDoacaoBinding
import com.google.android.material.navigation.NavigationView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File
import java.io.IOException
import java.util.Date
import kotlin.jvm.java


class MinhaDoacaoActitity : AppCompatActivity() {
    private val binding by lazy { ActivityMinhaDoacaoBinding.inflate(layoutInflater) }

    private val listaDoacao = ArrayList<InformacoesDoacao>()
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private val firestore = FirebaseFirestore.getInstance()

    // Declarando SharedPreferences
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var userUid: String

    private lateinit var progressDialog: ProgressDialog // Declare o ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        // Inicialize o ProgressDialog
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Carregando dados...") // Mensagem do ProgressDialog
        progressDialog.setCancelable(false) // Impede o cancelamento ao tocar fora do dialog

        // Inicializando SharedPreferences
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        userUid = getUserUid() ?: ""

        if (userUid.isEmpty()) {
            Toast.makeText(this, "Erro: UID do usuário não encontrado", Toast.LENGTH_SHORT).show()
            finish() // Fecha a activity se não tiver um UID válido
            return
        }

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)

        val adapter = CustomAdapter(this, listaDoacao)
        binding.listDoacao.adapter = adapter

        binding.ButtonAdicionar.setOnClickListener {
            val userUid = intent.getStringExtra("USER_UID") ?: "UID não disponível"
            val intent = Intent(this, AdicionarDoacaoActivity::class.java)
            intent.putExtra("USER_UID", userUid)
            startActivity(intent)
        }

        binding.listDoacao.setOnItemClickListener { _, _, position, _ ->

            val listaDoacaoSelecionada = listaDoacao[position]

            val intent = Intent(this, EditarDoacaoActivity::class.java).apply {
                putExtra("title", listaDoacaoSelecionada.titulo)
                putExtra("name", listaDoacaoSelecionada.nome)
                putExtra("desc", listaDoacaoSelecionada.descricao)

            }
            startActivity(intent)
        }

        binding.ButtonMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        navigationView.setNavigationItemSelectedListener { item ->
            handleNavigationItemSelected(item)
            drawerLayout.closeDrawer(GravityCompat.END)
            true
        }

        binding.buttonFiltroIndisponivel.setOnClickListener{
            fetchDoacoesIndisponivel()

        }

        binding.buttonFiltroDisponivel.setOnClickListener{
           fetchDoacoesDisponivel()
        }
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
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
                finish()
            }
            R.id.nav_Vizualizar_Perfil -> {
                val intent = Intent(this, ListagensUserActivity::class.java)
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
                val intent = Intent(this, SolicitacoesFeitasActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
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

    private fun fetchDoacoes() {
        // Exibe o ProgressDialog enquanto os dados estão sendo carregados
        progressDialog.show()

        firestore.collection("doacoes").whereEqualTo("userUid", userUid)
            .get()
            .addOnSuccessListener { querySnapshot ->
                listaDoacao.clear()

                if (!querySnapshot.isEmpty) {
                    for (document in querySnapshot.documents) {
                        val titulo = document.getString("titulo") ?: "Título desconhecido"
                        val descricao = document.getString("descricao") ?: "Sem descrição"
                        val data = document.getString("data")
                        val nomeUsuario = document.getString("nome") ?: "Usuário desconhecido"
                        val disponibilidade = document.getString("disponibilidade") ?: "Indisponível"
                        val latitude = document.getDouble("latitude") ?: 0.0
                        val longitude = document.getDouble("longitude") ?: 0.0
                        val primeiraImagemUrl = document.getString("primeiraImagemUrl")

                        val doacao = InformacoesDoacao(
                            titulo, nomeUsuario, descricao, longitude, latitude, data, disponibilidade, primeiraImagemUrl
                        )
                        listaDoacao.add(doacao)
                    }

                    (binding.listDoacao.adapter as? CustomAdapter)?.notifyDataSetChanged()
                } else {
                    Toast.makeText(this, "Nenhuma doação disponível.", Toast.LENGTH_SHORT).show()
                }

                progressDialog.dismiss()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Erro ao buscar doações: ${exception.message}", Toast.LENGTH_SHORT).show()
                progressDialog.dismiss()
            }
    }

    private fun fetchDoacoesDisponivel() {
        firestore.collection("doacoes")
            .whereEqualTo("userUid",userUid)
            .whereEqualTo("disponibilidade", "disponível") // Aqui já estamos buscando as doações disponíveis
            .get()
            .addOnSuccessListener { querySnapshot ->
                listaDoacao.clear()

                if (!querySnapshot.isEmpty) {
                    for (document in querySnapshot.documents) {
                        val titulo = document.getString("titulo") ?: "Título desconhecido"
                        val descricao = document.getString("descricao") ?: "Sem descrição"
                        val data = document.getString("data")
                        val nomeUsuario = document.getString("nome") ?: "Usuário desconhecido"
                        val disponibilidade = document.getString("disponibilidade") ?: "Indisponível"
                        val latitude = document.getDouble("latitude") ?: 0.0
                        val longitude = document.getDouble("longitude") ?: 0.0
                        val primeiraImagemUrl = document.getString("primeiraImagemUrl")

                        val doacao = InformacoesDoacao(
                            titulo, nomeUsuario, descricao, longitude, latitude, data, disponibilidade, primeiraImagemUrl
                        )
                        listaDoacao.add(doacao)
                    }

                    (binding.listDoacao.adapter as? CustomAdapter)?.notifyDataSetChanged()
                } else {
                    Toast.makeText(this, "Nenhuma doação disponível.", Toast.LENGTH_SHORT).show()
                }

                progressDialog.dismiss()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Erro ao buscar doações: ${exception.message}", Toast.LENGTH_SHORT).show()
                progressDialog.dismiss()
            }
    }

    private fun fetchDoacoesIndisponivel() {
        firestore.collection("doacoes")
            .whereEqualTo("userUid",userUid)
            .whereEqualTo("disponibilidade", "indisponível") // Aqui já estamos buscando as doações disponíveis
            .get()
            .addOnSuccessListener { querySnapshot ->
                listaDoacao.clear()

                if (!querySnapshot.isEmpty) {
                    for (document in querySnapshot.documents) {
                        val titulo = document.getString("titulo") ?: "Título desconhecido"
                        val descricao = document.getString("descricao") ?: "Sem descrição"
                        val data = document.getString("data")
                        val nomeUsuario = document.getString("nome") ?: "Usuário desconhecido"
                        val disponibilidade = document.getString("disponibilidade") ?: "Indisponível"
                        val latitude = document.getDouble("latitude") ?: 0.0
                        val longitude = document.getDouble("longitude") ?: 0.0
                        val primeiraImagemUrl = document.getString("primeiraImagemUrl")

                        val doacao = InformacoesDoacao(
                            titulo, nomeUsuario, descricao, longitude, latitude, data, disponibilidade, primeiraImagemUrl
                        )
                        listaDoacao.add(doacao)
                    }

                    (binding.listDoacao.adapter as? CustomAdapter)?.notifyDataSetChanged()
                } else {
                    Toast.makeText(this, "Nenhuma doação disponível.", Toast.LENGTH_SHORT).show()
                }

                progressDialog.dismiss()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Erro ao buscar doações: ${exception.message}", Toast.LENGTH_SHORT).show()
                progressDialog.dismiss()
            }
    }


    override fun onResume() {
        super.onResume()

        // Atualiza a lista de doações sempre que a Activity voltar ao primeiro plano
        fetchDoacoes()  // Recarregar os dados
    }


    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END) // Fecha o menu lateral, se aberto
        } else {
            super.onBackPressed() // Voltar ao comportamento padrão
        }
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
