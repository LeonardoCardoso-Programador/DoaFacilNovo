package com.example.doafacilnovo

import CustomAdapter
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
import com.example.doafacilnovo.databinding.ActivityMinhaDoacaoBinding
import com.google.android.material.navigation.NavigationView
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File
import java.io.IOException
import java.util.Date
import java.util.function.Consumer


class MinhaDoacaoActitity : AppCompatActivity() {
    private val binding by lazy { ActivityMinhaDoacaoBinding.inflate(layoutInflater) }

    private val listaDoacao = ArrayList<InformacoesDoacao>()
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private val firestore = FirebaseFirestore.getInstance()

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
            val intent = Intent(this, EditarDoacaoActivity::class.java)
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

    private fun handleNavigationItemSelected(item: MenuItem) {
        val userUid = intent.getStringExtra("USER_UID") ?: "UID não disponível"
        when (item.itemId) {
            R.id.nav_home -> {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_doacoes -> {
                drawerLayout.closeDrawer(GravityCompat.END) // Evita recriar a mesma activity
            }
        }
    }








    private fun fetchDoacoes() {
        firestore.collection("doacoes").whereEqualTo("userUid", userUid)
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

    private fun fetchDoacoesDisponivel() {
        firestore.collection("doacoes")
            .whereEqualTo("userUid",userUid)
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

    private fun fetchDoacoesIndisponivel() {
        firestore.collection("doacoes")
            .whereEqualTo("userUid",userUid)
            .whereEqualTo("disponibilidade", "indisponível") // Aqui já estamos buscando as doações disponíveis
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
