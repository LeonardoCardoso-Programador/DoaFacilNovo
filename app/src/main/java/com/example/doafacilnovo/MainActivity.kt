package com.example.doafacilnovo

import CustomAdapter
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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File
import java.io.IOException
import java.util.*



class MainActivity : AppCompatActivity() {


    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val listaDoacao = ArrayList<InformacoesDoacao>()
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private val firestore = FirebaseFirestore.getInstance()  // Inicializa o Firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Inicializando o DrawerLayout e NavigationView
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)

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
    }



    // Função que trata as ações de navegação
    private fun handleNavigationItemSelected(item: MenuItem) {

        val userUid = intent.getStringExtra("USER_UID") ?: "UID nao disponivel"
        when (item.itemId) {
            R.id.nav_home -> {
                // Ação para a opção "Home"
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }

            R.id.nav_doacoes -> {
                val intent = Intent(this, MinhaDoacaoActitity::class.java)
                intent.putExtra("USER_UID", userUid)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
            }

            R.id.nav_services -> {
                // Ação para a opção "Serviços"
            }

            R.id.nav_contact -> {
                // Ação para a opção "Contato"
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
        firestore.collection("doacoes")
            .whereEqualTo("disponibilidade", "disponível") // Aqui já estamos buscando as doações disponíveis
            .get()
            .addOnSuccessListener { querySnapshot ->
                listaDoacao.clear()  // Limpa a lista antes de adicionar novos dados

                if (!querySnapshot.isEmpty) {
                    val doacoes = mutableListOf<InformacoesDoacao>() // Lista temporária para armazenar as doações
                    var doacoesProcessadas = 0 // Contador de doações processadas

                    // Itera sobre os documentos das doações
                    for (document in querySnapshot.documents) {
                        val titulo = document.getString("titulo") ?: "Título desconhecido"
                        val descricao = document.getString("descricao") ?: "Sem descrição"
                        val data = document.getString("data")
                        val nomeUsuario = document.getString("nome") ?: "Usuário desconhecido"
                        val disponibilidade = document.getString("disponibilidade") ?: "Indisponível"
                        val latitude = document.getDouble("latitude") ?: 0.0
                        val longitude = document.getDouble("longitude") ?: 0.0
                        val pastaID = document.getString("pastaID") // Pasta ID para associar as imagens
                        val imagemUrls = document.get("imagemUrls") as? List<String> ?: emptyList()  // Lista de URLs das imagens

                        // Aqui, chamamos a função para buscar apenas a primeira imagem
                        pastaID?.let {
                            if (imagemUrls.isNotEmpty()) {
                                val primeiraImagemUrl = imagemUrls.first()

                                // Buscar apenas a primeira imagem
                                fetchPrimeiraImagem(primeiraImagemUrl) { primeiraImagem ->
                                    // Cria o objeto da doação com a primeira imagem
                                    val doacao = InformacoesDoacao(
                                        titulo, nomeUsuario, descricao, longitude, latitude, data, disponibilidade, primeiraImagem
                                    )

                                    // Adiciona a doação na lista temporária
                                    doacoes.add(doacao)

                                    // Incrementa o contador de doações processadas
                                    doacoesProcessadas++

                                    // Se todas as doações foram processadas (última doação), atualiza a lista de doações
                                    if (doacoesProcessadas == querySnapshot.documents.size) {
                                        listaDoacao.addAll(doacoes) // Adiciona todas as doações à lista final
                                        // Atualiza o adapter após adicionar a doação
                                        if (binding.listDoacao.adapter == null) {
                                            binding.listDoacao.adapter = CustomAdapter(this, listaDoacao)
                                        } else {
                                            (binding.listDoacao.adapter as CustomAdapter).notifyDataSetChanged()
                                        }
                                    }
                                }
                            } else {
                                // Caso não haja imagem
                                doacoesProcessadas++
                            }
                        }
                    }
                } else {
                    // Se não houver doações disponíveis
                    Toast.makeText(this, "Nenhuma doação disponível.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Erro ao buscar doações: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }




    private fun fetchPrimeiraImagem(imagemUrl: String, callback: (Bitmap?) -> Unit) {
        val storageReference: StorageReference = FirebaseStorage.getInstance().getReferenceFromUrl(imagemUrl)

        // Criar um arquivo temporário para armazenar a imagem
        val localFile = File.createTempFile("primeiraImagem", "jpg")

        storageReference.getFile(localFile).addOnSuccessListener {
            try {
                // Converte o arquivo baixado para um Bitmap
                val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)
                callback(bitmap)  // Retorna a primeira imagem (Bitmap)
            } catch (e: IOException) {
                e.printStackTrace()
                callback(null)  // Caso haja erro na conversão
            }
        }.addOnFailureListener { exception ->
            // Caso haja erro ao baixar a imagem
            Toast.makeText(this, "Erro ao baixar a imagem: ${exception.message}", Toast.LENGTH_SHORT).show()
            callback(null)  // Retorna null caso haja erro
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



