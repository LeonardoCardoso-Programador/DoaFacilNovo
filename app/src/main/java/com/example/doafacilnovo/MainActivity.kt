package com.example.doafacilnovo

import CustomAdapter
import android.app.ProgressDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.doafacilnovo.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView
import com.google.firebase.firestore.FirebaseFirestore
import android.content.Context
import android.util.Log
import com.example.doafacilnovo.Login_Registro.LoginActivity
import java.util.*
import kotlin.jvm.java


class MainActivity : AppCompatActivity() {


    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val listaDoacao = ArrayList<InformacoesDoacao>()
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private val firestore = FirebaseFirestore.getInstance()  // Inicializa o Firestore
    private lateinit var progressDialog: ProgressDialog // Declare o ProgressDialog
    private lateinit var userUid: String
    private lateinit var sharedPreferences: SharedPreferences


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Inicialize o ProgressDialog
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Carregando dados...") // Mensagem do ProgressDialog
        progressDialog.setCancelable(false) // Impede o cancelamento ao tocar fora do dialog

        // Inicializando o DrawerLayout e NavigationView
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)

        // Inicializando SharedPreferences
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        userUid = getUserUid() ?: ""

        // Criando e configurando o adapter
        val adapter = CustomAdapter(this, listaDoacao)
        binding.listDoacao.adapter = adapter

        // Ação do botão Adicionar (abre a activity AdicionarDoacaoActivity)
        binding.ButtonAdicionar.setOnClickListener {
            val intent = Intent(this, AdicionarDoacaoActivity::class.java)
            startActivity(intent) // Usando o novo contrato
        }

        // Ação do clique na lista (abre a activity SolicitarDoacaoActivity)
        binding.listDoacao.setOnItemClickListener { _, _, position, _ ->
            val listaDoacaoSelecionada = listaDoacao[position]

            val intent = Intent(this, SolicitarDoacaoActivity::class.java).apply {
                putExtra("title", listaDoacaoSelecionada.titulo)
                putExtra("name", listaDoacaoSelecionada.nome)
                putExtra("desc", listaDoacaoSelecionada.descricao)

            }
            startActivity(intent)  // Código atualizado para Android 13+ sem `startActivityForResult`

        }

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

    }

    override fun onResume() {
        super.onResume()

        // Recarregar as doações apenas, sem recriar o adapter
        fetchDoacoes()

        verificarNovasSolicitacoes(userUid)
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
                true
            }

            R.id.nav_doacoes -> {
                val intent = Intent(this, MinhaDoacaoActitity::class.java)
                intent.putExtra("USER_UID", userUid)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
            }

            R.id.nav_MapaLoc -> {

                val intent = Intent(this, LocalizacaoActivity::class.java)
                intent.putExtra("USER_UID", userUid)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)

            }

            R.id.nav_Perfil -> {
                val intent = Intent(this, PerfilActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
            }
            R.id.nav_Vizualizar_Perfil -> {
                val intent = Intent(this, ListagensUserActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
            }
            R.id.nav_Solicitacoes -> {
                val intent = Intent(this, SolicitacoesRecebidasActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
            }
            R.id.nav_SolicitacoesFeitas -> {
                val intent = Intent(this, SolicitacoesFeitasActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
            }
            R.id.nav_Logout ->{
                finish()
            }
            else -> {
                // Caso não haja nenhuma ação definida para o item, nada acontece
            }
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END) // Fecha o drawer se estiver aberto
        } else {
            super.onBackPressed() // Comporta-se normalmente se o drawer estiver fechado
        }
    }

    private fun fetchDoacoes() {
        progressDialog.show()

        firestore.collection("doacoes").whereEqualTo("disponibilidade", "disponível")
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



    /*private fun verificarExpiracaoDoacoes() {
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
    }*/

    private fun verificarNovasSolicitacoes(userUid: String) {
        val userRef = firestore.collection("users").document(userUid)

        // Listener em tempo real
        userRef.addSnapshotListener { document, e ->
            if (e != null) {
                Log.w("Firestore", "Listen failed.", e)
                return@addSnapshotListener
            }

            if (document != null && document.exists()) {
                val solicitacoesRecebidas = document.getLong("solicitacoesRecebidas") ?: 0
                val compararSolicitacao = document.getLong("compararSolicitacao") ?: 0

                if (solicitacoesRecebidas > compararSolicitacao && !isFinishing) {
                    mostrarDialogNovasSolicitacoes(userUid, solicitacoesRecebidas, compararSolicitacao)
                }

            } else {
                runOnUiThread {
                    Toast.makeText(this, "Usuário não encontrado.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }



    private fun mostrarDialogNovasSolicitacoes(userUid: String, novasSolicitacoes: Long, compararSolicitacoes: Long) {
        if (!isFinishing && !isDestroyed) { // Verifica se a Activity ainda está ativa
            val valorNovaSolicitacoes = novasSolicitacoes - compararSolicitacoes
            val mensagem = "Você tem +$valorNovaSolicitacoes novas solicitações recebidas!"

            val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Novas Solicitações")
                .setMessage(mensagem)
                .setPositiveButton("OK") { dialog, _ ->
                    atualizarCompararSolicitacao(userUid, novasSolicitacoes)
                    dialog.dismiss()
                }
                .setNegativeButton("Ir para Solicitações") { dialog, _ ->
                    val intent = Intent(this, SolicitacoesRecebidasActivity::class.java)
                    startActivity(intent)
                    atualizarCompararSolicitacao(userUid, novasSolicitacoes)
                    dialog.dismiss()
                }
                .create()

            dialog.show()
        } else {
            Log.w("Dialog", "A Activity foi finalizada antes de exibir o diálogo.")
        }
    }



    private fun atualizarCompararSolicitacao(userUid: String, novoValor: Long) {
        val userRef = firestore.collection("users").document(userUid)

        userRef.update("compararSolicitacao", novoValor)
            .addOnSuccessListener {
                Log.d("Atualização", "compararSolicitacao atualizado para $novoValor")
            }
            .addOnFailureListener { e ->
                Log.e("Erro", "Falha ao atualizar compararSolicitacao: ${e.message}")
            }
    }




}



